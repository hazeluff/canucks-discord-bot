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
import com.hazeluff.nhl.event.GameEvent;
import com.hazeluff.nhl.event.GoalEvent;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Displays the score of a game in a Game Day Channel.
 */
public class GDCCommand extends Command {
	static final String NAME = "gdc";

	public GDCCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	public String getName() {
		return NAME;
	}

	public ApplicationCommandRequest getACR() {
		return ApplicationCommandRequest.builder()
				.name(getName())
				.description("Get the score of the current game. Use only in Game Day Channels.")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("subcommand")
						.description("Subcommand to execute. Help: `/gdc subcommand: help`")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
				.build();
	}

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {

		TextChannel channel = getChannel(event);
		Game game = nhlBot.getGameScheduler().getGameByChannelName(channel.getName());
		if (game == null) {
			// Not in game day channel
			return event.reply(HELP_MESSAGE);
		}

		String strSubcommand = getOptionAsString(event, "subcommand");
		if (strSubcommand == null) {
			// No option specified
			return event.reply(HELP_MESSAGE);
		}
		switch (strSubcommand) {
		case "score":
			return replyToScoreCommand(event, game);
		case "goals":
			return replyToGoalsCommand(event, game);
		case "help":
		default:
			return event.reply(HELP_MESSAGE);
		}
	}

	/*
	 * General
	 */
	private static final Consumer<? super InteractionApplicationCommandCallbackSpec> HELP_MESSAGE = 
			callbackSpec -> callbackSpec
					.addEmbed(embedSpec -> embedSpec
							.setTitle("Game Day Channel - Commands")
							.setDescription("Use `/gdc subcommand:` in to get live data about the current game."
									+ " Must be used in a Game Day Channel.")
							.addField("score", "Get the game's current score.", false)
							.addField("goals", "Get the game's current score and goals.", false)
					)
					.setEphemeral(true);
	

	private static final Consumer<? super InteractionApplicationCommandCallbackSpec> GAME_NOT_STARTED_MESSAGE = 
			callbackSpec -> callbackSpec
					.setContent("The game hasn't started yet.")
					.setEphemeral(true);			

	/*
	 * Score
	 */
	private Publisher<?> replyToScoreCommand(ChatInputInteractionEvent event, Game game) {
		if (!game.getStatus().isStarted()) {
			return event.reply(GAME_NOT_STARTED_MESSAGE);
		}

		return event.reply(callbackSpec -> callbackSpec.addEmbed(embedSpec -> buildScoreEmbed(embedSpec, game)));
	}
	
	public static EmbedCreateSpec buildScoreEmbed(EmbedCreateSpec embedSpec, Game game) {
		String homeGoals = "Goals:  **" + game.getHomeScore() + "**";
		String awayGoals = "Goals:  **" + game.getAwayScore() + "**";
		return embedSpec
				.addField(
						game.getHomeTeam().getFullName(),
						"Home\n" + homeGoals,
						true
				)
				.addField(
						"vs",
						"~~", // For formatting
						true
				)
				.addField(
						game.getAwayTeam().getFullName(),
						"Away\n" + awayGoals,
						true
				);
	}

	/*
	 * Goals
	 */
	private Publisher<?> replyToGoalsCommand(ChatInputInteractionEvent event, Game game) {
		if (!game.getStatus().isStarted()) {
			return event.reply(GAME_NOT_STARTED_MESSAGE);
		}

		return event.reply(callbackSpec -> callbackSpec.addEmbed(embedSpec -> buildGoalsEmbed(embedSpec, game)));
	}

	public static EmbedCreateSpec buildGoalsEmbed(EmbedCreateSpec spec, Game game) {
		List<GoalEvent> goals = game.getScoringEvents();
		EmbedCreateSpec embedSpec = buildScoreEmbed(spec, game);
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
		return embedSpec;
	}

	/**
	 * Builds the details to be displayed.
	 * 
	 * @return details as formatted string
	 */
	private static String buildGoalLine(GameEvent gameEvent) {
		StringBuilder details = new StringBuilder();
		List<Player> players = gameEvent.getPlayers();
		details.append(String.format("**%s** @ %s - **%-18s**", gameEvent.getTeam().getCode(),
				gameEvent.getPeriodTime(), players.get(0).getFullName()));
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
