package com.hazeluff.discord.bot.command;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.GameDayChannel;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.nhl.Game;
import com.hazeluff.discord.nhl.GameScheduler;
import com.hazeluff.discord.nhl.GameStatus;
import com.hazeluff.discord.nhl.Team;

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
				return event.replyEphemeral(SUBSCRIBE_FIRST_MESSAGE + "\n\n"
						+ "Alternatively you can choose a specific team's schedule to view.");
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

	Consumer<InteractionApplicationCommandCallbackSpec> getScheduleMessage(Team team) {
		String message = "Here is the schedule for the " + team.getFullName();

		GameScheduler gameScheduler = nhlBot.getGameScheduler();
		List<Consumer<EmbedCreateSpec>> embedAppends = new ArrayList<>();

		for (int i = 1; i >= 0; i--) {
			Game game = gameScheduler.getPastGame(team, i);
			if (game != null) {
				embedAppends.add(getEmbedGameAppend(game, team, GameState.PAST));
			}
		}
		
		Game currentGame = gameScheduler.getCurrentGame(team);

		if (currentGame != null) {
			embedAppends.add(getEmbedGameAppend(currentGame, team, GameState.CURRENT));
		}
		
		int futureGames = currentGame == null ? 4 : 3;
		for (int i = 0; i < futureGames; i++) {
			Game game = gameScheduler.getFutureGame(team, i);
			if (game == null) {
				break;
			}
			if (currentGame == null && i == 0) {
				embedAppends.add(getEmbedGameAppend(game, team, GameState.NEXT));
			} else {
				embedAppends.add(getEmbedGameAppend(game, team, GameState.FUTURE));

			}
		}

		return spec -> spec
				.setContent(message)
				.addEmbed(embed -> {
					embed.setColor(team.getColor());
					embedAppends.forEach(e -> e.accept(embed));
				});
	}

	Consumer<InteractionApplicationCommandCallbackSpec> getScheduleMessage(List<Team> teams) {
		String message = "Here is the schedule for all your teams. "
				+ "Use `?schedule [team] to get more detailed schedules.";

		GameScheduler gameScheduler = nhlBot.getGameScheduler();
		List<Consumer<EmbedCreateSpec>> embedAppends = new ArrayList<>();
		for (Team team : teams) {
			Game currentGame = gameScheduler.getCurrentGame(team);

			if (currentGame != null) {
				embedAppends.add(getEmbedGameAppend(currentGame, team, GameState.CURRENT));
			}

			int futureGames = currentGame == null ? 2 : 1;
			for (int i = 0; i < futureGames; i++) {
				Game game = gameScheduler.getFutureGame(team, i);
				if (game == null) {
					break;
				}
				if (currentGame == null && i == 0) {
					embedAppends.add(getEmbedGameAppend(game, team, GameState.NEXT));
				} else {
					embedAppends.add(getEmbedGameAppend(game, team, GameState.FUTURE));
				}
			}
		}

		return spec -> spec
				.setContent(message)
				.addEmbed(embed -> embedAppends.forEach(e -> e.accept(embed)));
	}

	enum GameState {
		PAST, CURRENT, NEXT, FUTURE;
	}

	Consumer<EmbedCreateSpec> getEmbedGameAppend(Game game, Team preferedTeam, GameState state) {
		ZoneId timeZone = preferedTeam.getTimeZone();
		StringBuilder date = new StringBuilder(GameDayChannel.getNiceDate(game, timeZone));
		String message;
		Function<Game, String> getAgainstTeamMessage = g -> {
			return g.getHomeTeam() == preferedTeam
					? String.format("vs %s", g.getAwayTeam().getFullName())
					: String.format("@ %s", g.getHomeTeam().getFullName());
		};

		// Add Time
		date.append(" at ").append(GameDayChannel.getTime(game, preferedTeam.getTimeZone()));

		switch(state) {
		case PAST:
			message = GameDayChannel.getScoreMessage(game);
			break;
		case CURRENT:
			if (game.getStatus() == GameStatus.POSTPONED) {
				date.append(" **(postponed)**");
			} else {
				date.append(" **(current game)**");
			}
			message = GameDayChannel.getScoreMessage(game);
			break;
		case NEXT:
			if (game.getStatus() == GameStatus.POSTPONED) {
				date.append(" **(postponed)**");
			} else {
				date.append(" **(next game)**");
			}
			message = preferedTeam.getFullName() + " " + getAgainstTeamMessage.apply(game);
			break;
		case FUTURE:
			if (game.getStatus() == GameStatus.POSTPONED) {
				date.append(" **(postponed)**");
			}
			message = preferedTeam.getFullName() + " " + getAgainstTeamMessage.apply(game);
			break;
		default:
			message = "";
			break;
		}

		return embed -> embed.addField(date.toString(), message, false);
	}

}
