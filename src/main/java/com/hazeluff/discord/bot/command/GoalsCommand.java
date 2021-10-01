package com.hazeluff.discord.bot.command;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.nhl.Game;
import com.hazeluff.discord.nhl.GameEvent;
import com.hazeluff.discord.nhl.GamePeriod;
import com.hazeluff.discord.nhl.GameStatus;
import com.hazeluff.discord.nhl.Team;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Displays all the goals in game of a 'Game Day Channel'
 */
public class GoalsCommand extends Command {
	static final String NAME = "goals";

	public GoalsCommand(NHLBot nhlBot) {
		super(nhlBot);
	}
	
	public String getName() {
		return NAME;
	}
	
	public ApplicationCommandRequest getACR() {
		return ApplicationCommandRequest.builder()
				.name(getName())
                .description("Get a list of all goals scored this game. Only available in a Game Day Channel.")
                .build();
	}

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {
		Snowflake guildId = event.getInteraction().getGuildId().get();
		List<Team> preferredTeams = nhlBot.getPersistentData().getPreferencesData()
				.getGuildPreferences(guildId.asLong()).getTeams();
		if (preferredTeams.isEmpty()) {
			return event.replyEphemeral(SUBSCRIBE_FIRST_MESSAGE);
		}

		TextChannel channel = getChannel(event);
		Game game = nhlBot.getGameScheduler().getGameByChannelName(channel.getName());
		if (game == null) {
			return event.replyEphemeral(getRunInGameDayChannelsMessage(getGuild(event), preferredTeams));
		}

		if (game.getStatus() == GameStatus.PREVIEW) {
			return event.replyEphemeral(GAME_NOT_STARTED_MESSAGE);
		}

		return event.reply(spec -> spec.addEmbed(getEmbed(game)));
	}

	public static String getGoalsMessage(Game game) {
		List<GameEvent> goals = game.getEvents();
		StringBuilder response = new StringBuilder();
		response.append("```\n");
		for (int i = 1; i <= 3; i++) {
			switch (i) {
			case 1:
				response.append("1st Period:");
				break;
			case 2:
				response.append("\n\n2nd Period:");
				break;
			case 3:
				response.append("\n\n3rd Period:");
				break;
			}
			int period = i;
			Predicate<GameEvent> isPeriod = gameEvent -> gameEvent.getPeriod().getPeriodNum() == period;
			if (goals.stream().anyMatch(isPeriod)) {
				for (GameEvent gameEvent : goals.stream().filter(isPeriod).collect(Collectors.toList())) {
					response.append("\n").append(gameEvent.getDetails());
				}
			} else {
				response.append("\nNone");
			}
		}
		Predicate<GameEvent> isOtherPeriod = gameEvent -> gameEvent.getPeriod().getPeriodNum() > 3;
		if (goals.stream().anyMatch(isOtherPeriod)) {
			GameEvent gameEvent = goals.stream().filter(isOtherPeriod).findFirst().get();
			GamePeriod period = gameEvent.getPeriod();
			response.append("\n\n").append(period.getDisplayValue()).append(":");
			goals.stream().filter(isOtherPeriod).forEach(event -> response.append("\n").append(event.getDetails()));
		}
		response.append("\n```");
		return response.toString();
	}

	public static Consumer<EmbedCreateSpec> getEmbed(Game game) {
		return spec -> ScoreCommand.buildEmbed(spec, game)
				.addField("First Period", "Test", false);
	}

}
