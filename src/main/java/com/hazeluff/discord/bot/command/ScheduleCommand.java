package com.hazeluff.discord.bot.command;

import java.time.ZoneId;
import java.util.List;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.gdc.GameDayChannel;
import com.hazeluff.discord.nhl.GameScheduler;
import com.hazeluff.nhl.Team;
import com.hazeluff.nhl.game.DetailedGameState;
import com.hazeluff.nhl.game.Game;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Lists the closest 10 games (5 previous, 5 future).
 */
public class ScheduleCommand extends Command {
	static final String NAME = "schedule";

	public ScheduleCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	public String getName() {
		return NAME;
	}

	public ApplicationCommandRequest getACR() {
		return ApplicationCommandRequest.builder()
				.name(getName())
				.description("Get the upcoming schedule of your subscribed (or defined) teams.")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("team")
                        .description("Which team to get the game schedule for.")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(false)
                        .build())
				.build();
	}

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {
		String strTeam = getOptionAsString(event, "team");
		if (strTeam == null) {
			Snowflake guildId = event.getInteraction().getGuildId().get();
			List<Team> preferredTeams = nhlBot.getPersistentData().getPreferencesData()
					.getGuildPreferences(guildId.asLong()).getTeams();

			if (preferredTeams.isEmpty()) {
				return event
						.reply(SUBSCRIBE_FIRST_MESSAGE
								+ "\n\nAlternatively you can choose a specific team's schedule to view.")
						.withEphemeral(true);
			}
			return event.reply(getScheduleMessage(preferredTeams));
		}

		if (!Team.isValid(strTeam)) {
			return event.reply(getInvalidTeamCodeMessage(strTeam));
		}
		return event.reply(getScheduleMessage(Team.parse(strTeam)));
	}

	static final String HELP_MESSAGE = 
			"Get the game schedule any of the following teams by typing `@NHLBot schedule [team]`, "
					+ "where [team] is the one of the three letter codes for your team below: "
					+ getTeamsListBlock();

	InteractionApplicationCommandCallbackSpec getScheduleMessage(Team team) {
		GameScheduler gameScheduler = nhlBot.getGameScheduler();
		EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();
		embedBuilder.color(team.getColor());

		for (Game game : gameScheduler.getPastGames(team, 1)) {
			appendGameToEmbed(embedBuilder, game, team, GameState.PAST);
		}
		
		Game currentGame = gameScheduler.getCurrentLiveGame(team);

		if (currentGame != null) {
			appendGameToEmbed(embedBuilder, currentGame, team, GameState.CURRENT);
		}
		
		int numFutureGames = currentGame == null ? 4 : 3;
		boolean isNext = true;
		for (Game game : gameScheduler.getFutureGames(team, numFutureGames)) {
			if (currentGame == null && isNext) {
				appendGameToEmbed(embedBuilder, game, team, GameState.NEXT);
				isNext = false;
			} else {
				appendGameToEmbed(embedBuilder, game, team, GameState.FUTURE);
			}
		}

		return InteractionApplicationCommandCallbackSpec.builder()
				.content("Here is the schedule for the " + team.getFullName())
				.addEmbed(embedBuilder.build())
				.build();
	}

	InteractionApplicationCommandCallbackSpec getScheduleMessage(List<Team> teams) {
		GameScheduler gameScheduler = nhlBot.getGameScheduler();
		EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();
		if (teams.size() == 1) {
			embedBuilder.color(teams.get(0).getColor());
		}
		for (Team team : teams) {
			for (Game game : gameScheduler.getPastGames(team, 1)) {
				appendGameToEmbed(embedBuilder, game, team, GameState.PAST);
			}

			Game currentGame = gameScheduler.getCurrentLiveGame(team);

			if (currentGame != null) {
				appendGameToEmbed(embedBuilder, currentGame, team, GameState.CURRENT);
			}

			int numFutureGames = currentGame == null ? 2 : 1;
			boolean isNext = true;
			for (Game game : gameScheduler.getFutureGames(team, numFutureGames)) {
				if (currentGame == null && isNext) {
					appendGameToEmbed(embedBuilder, game, team, GameState.NEXT);
					isNext = false;
				} else {
					appendGameToEmbed(embedBuilder, game, team, GameState.FUTURE);

				}
			}
		}

		return InteractionApplicationCommandCallbackSpec.builder()
				.content("Here is the schedule for all your teams. You may use the `team` option to specify a team.")
				.addEmbed(embedBuilder.build())
				.build();
	}

	enum GameState {
		PAST, CURRENT, NEXT, FUTURE;
	}

	EmbedCreateSpec.Builder appendGameToEmbed(EmbedCreateSpec.Builder builder, Game game, Team preferedTeam,
			GameState state) {
		ZoneId timeZone = preferedTeam.getTimeZone();
		StringBuilder date = new StringBuilder(GameDayChannel.getNiceDate(game, timeZone));
		String message;
		Function<Game, String> getAgainstTeamMessage = g -> {
			return g.getHomeTeam() == preferedTeam
					? String.format("vs %s", g.getAwayTeam().getFullName())
					: String.format("@ %s", g.getHomeTeam().getFullName());
		};

		// Add Time
		date.append("  at  ").append(GameDayChannel.getTime(game, preferedTeam.getTimeZone()));

		switch(state) {
		case PAST:
			message = buildGameScore(game);
			break;
		case CURRENT:
			if (game.getStatus().getDetailedState() == DetailedGameState.POSTPONED) {
				date.append(" (postponed)");
			} else if (game.getStatus().getDetailedState() == DetailedGameState.POSTPONED) {
				date.append(" **(LIVE)**");
			} else {
				date.append(" (current game)");
			}
			message = buildGameScore(game);
			break;
		case NEXT:
			if (game.getStatus().getDetailedState() == DetailedGameState.POSTPONED) {
				date.append(" (postponed)");
			} else {
				date.append(" (next game)");
			}
			message = preferedTeam.getFullName() + " " + getAgainstTeamMessage.apply(game);
			break;
		case FUTURE:
			if (game.getStatus().getDetailedState() == DetailedGameState.POSTPONED) {
				date.append(" (postponed)");
			}
			message = preferedTeam.getFullName() + " " + getAgainstTeamMessage.apply(game);
			break;
		default:
			message = "";
			break;
		}

		return builder.addField(date.toString(), message, false);
	}

	private static String buildGameScore(Game game) {
		return String.format("%s **%s** - **%s** %s", 
				game.getHomeTeam().getName(), game.getHomeScore(),
				game.getAwayScore(), game.getAwayTeam().getName());
	}
}
