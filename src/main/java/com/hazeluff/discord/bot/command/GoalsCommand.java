package com.hazeluff.discord.bot.command;

import java.util.List;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.GameDayChannel;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.nhl.Game;
import com.hazeluff.discord.nhl.GameStatus;
import com.hazeluff.discord.nhl.Team;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.channel.TextChannel;
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

		return event.reply(getGoalsMessage(game));
	}

	public String getGoalsMessage(Game game) {
		return String.format("%s\n%s", GameDayChannel.getScoreMessage(game), GameDayChannel.getGoalsMessage(game));
	}

}
