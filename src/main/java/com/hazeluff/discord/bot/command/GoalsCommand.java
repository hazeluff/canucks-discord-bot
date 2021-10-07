package com.hazeluff.discord.bot.command;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.nhl.Game;
import com.hazeluff.nhl.GameStatus;
import com.hazeluff.nhl.Player;
import com.hazeluff.nhl.Team;
import com.hazeluff.nhl.event.GameEvent;
import com.hazeluff.nhl.event.GoalEvent;

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

		if (!game.getStatus().isStarted()) {
			return event.replyEphemeral(GAME_NOT_STARTED_MESSAGE);
		}

		return event.reply(spec -> spec.addEmbed(getEmbed(game)));
	}

	public static Consumer<EmbedCreateSpec> getEmbed(Game game) {
		return spec -> { 
			List<GoalEvent> goals = game.getScoringEvents();
			EmbedCreateSpec embedSpec = ScoreCommand.buildEmbed(spec, game);
			// Regulation Periods
			for (int period = 1; period <= 3; period++) {
				String strPeriod = ""; // Field Title
				switch (period) {
				case 1:
					strPeriod = "1st Period";
					break;
				case 2:
					strPeriod = "2nd Period";
					break;
				case 3:
					strPeriod = "3rd Period";
					break;
				}
				String strGoals = ""; // Field Body
				int fPeriod = period;
				Predicate<GameEvent> isPeriod = gameEvent -> gameEvent.getPeriod().getPeriodNum() == fPeriod;
				if (goals.stream().anyMatch(isPeriod)) {
					List<GameEvent> periodGoals = goals.stream().filter(isPeriod).collect(Collectors.toList());
					StringBuilder sbGoals = new StringBuilder();
					for (GameEvent gameEvent : periodGoals) {
						if (sbGoals.length() != 0) {
							sbGoals.append("\n");
						}
						sbGoals.append(buildGoalLine(gameEvent));
					}
					strGoals = sbGoals.toString();
				} else {
					strGoals = "None";
				}
				embedSpec.addField(strPeriod, strGoals, false);
			}
			// Overtime and Shootouts
			Predicate<GameEvent> isExtraPeriod = gameEvent -> gameEvent.getPeriod().getPeriodNum() > 3;
			if (goals.stream().anyMatch(isExtraPeriod)) {
				List<GameEvent> extraPeriodGoals = goals.stream().filter(isExtraPeriod).collect(Collectors.toList());
				String strPeriod = extraPeriodGoals.get(0).getPeriod().getDisplayValue();
				StringBuilder sbGoals = new StringBuilder();
				for (GameEvent goal : extraPeriodGoals) {
					if (sbGoals.length() == 0) {
						sbGoals.append("\n");
					}
					sbGoals.append(buildGoalLine(goal));
				}
				embedSpec.addField(strPeriod, sbGoals.toString(), false);
			}

			GameStatus status = game.getStatus();
			embedSpec.setFooter("Status: " + status.getDetailedState().toString(), null);
		};
	}

	/**
	 * Builds the details to be displayed.
	 * 
	 * @return details as formatted string
	 */
	public static String buildGoalLine(GameEvent gameEvent) {
		StringBuilder details = new StringBuilder();
		List<Player> players = gameEvent.getPlayers();
		details.append(String.format("**%s** @ %s - **%-18s**",
				gameEvent.getTeam().getCode(), gameEvent.getPeriodTime(), players.get(0).getFullName()));
		if (players.size() > 1) {
			details.append("  Assists: ");
			details.append(players.get(1).getFullName());
		}
		if (players.size() > 2) {
			details.append(", ");
			details.append(players.get(2).getFullName());
		}
		return details.toString();
	}
}
