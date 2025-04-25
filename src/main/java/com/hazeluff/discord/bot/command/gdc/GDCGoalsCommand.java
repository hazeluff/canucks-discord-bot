package com.hazeluff.discord.bot.command.gdc;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.Command;
import com.hazeluff.nhl.game.Game;
import com.hazeluff.nhl.game.GameState;
import com.hazeluff.nhl.game.RosterPlayer;
import com.hazeluff.nhl.game.event.GameEvent;
import com.hazeluff.nhl.game.event.GoalEvent;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.EmbedCreateSpec.Builder;

public class GDCGoalsCommand extends GDCScoreCommand {

	@Override
	public String getName() {
		return "goals";
	}

	@Override
	public String getDescription() {
		return "List of goals and their players.";
	}

	@Override
	public Publisher<?> reply(ChatInputInteractionEvent event, NHLBot nhlBot, Game game) {
		if (!game.getGameState().isStarted()) {
			return Command.reply(event, GAME_NOT_STARTED_MESSAGE, true);
		}

		Builder embedBuilder = EmbedCreateSpec.builder();
		// Add Score
		GDCScoreCommand.buildEmbed(embedBuilder, game);
		// Add Goals
		buildEmbed(embedBuilder, game);
		return Command.reply(event, embedBuilder.build());
	}

	public static EmbedCreateSpec getEmbed(Game game) {
		Builder embedBuilder = EmbedCreateSpec.builder();
		buildEmbed(embedBuilder, game);
		return embedBuilder.build();
	}

	public static EmbedCreateSpec.Builder buildEmbed(EmbedCreateSpec.Builder embedBuilder, Game game) {
		List<GoalEvent> goals = game.getScoringEvents();
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
			Predicate<GameEvent> isPeriod = gameEvent -> gameEvent.getPeriod() == fPeriod;
			if (goals.stream().anyMatch(isPeriod)) {
				List<String> strPeriodGoals = goals.stream()
						.filter(isPeriod)
						.map(goalEvent -> buildGoalLine(game, goalEvent))
						.collect(Collectors.toList());
				strGoals = String.join("\n", strPeriodGoals);
			} else {
				strGoals = "None";
			}
			embedBuilder.addField(strPeriod, strGoals, false);
		}
		// Overtime and Shootouts
		Predicate<GameEvent> isExtraPeriod = gameEvent -> gameEvent.getPeriod() > 3;
		if (goals.stream().anyMatch(isExtraPeriod)) {
			List<String> strExtraPeriodGoals = goals.stream()
					.filter(isExtraPeriod)
					.map(goalEvent -> buildGoalLine(game, goalEvent))
					.collect(Collectors.toList());
			String periodCode = game.getGameType().getPeriodCode(
					goals.stream().filter(isExtraPeriod).findAny().get().getPeriod());
			String strGoals = String.join("\n", strExtraPeriodGoals);
			embedBuilder.addField(periodCode, strGoals, false);
		}

		GameState status = game.getGameState();
		embedBuilder.footer("Status: " + status.toString(), null);
		return embedBuilder;
	}

	/**
	 * Builds the details to be displayed.
	 * 
	 * @return details as formatted string
	 */
	private static String buildGoalLine(Game game, GoalEvent goalEvent) {
		StringBuilder details = new StringBuilder();
		RosterPlayer scorer = game.getPlayer(goalEvent.getScorerId());
		if(scorer != null) {
			if (!game.getGameType().isShootout(goalEvent.getPeriod())) {
				List<RosterPlayer> assists = goalEvent.getAssistIds().stream()
						.map(game::getPlayer)
						.collect(Collectors.toList());
				details.append(String.format("**%s** @ %s - **%-18s**", 
						scorer.getTeam().getCode(), goalEvent.getPeriodTime(), scorer.getFullName()));
				if (assists.size() > 0) {
					details.append("  Assists: ");
					details.append(assists.get(0).getFullName());
				}
				if (assists.size() > 1) {
					details.append(", ");
					details.append(assists.get(1).getFullName());
				}
			} else {
				details.append(String.format("**%s** - **%-18s**", 
						scorer.getTeam().getCode(), scorer.getFullName()));
			}
		} else {
			details.append(String.format("**%s** - (%-18s)", 
					goalEvent.getTeam().getCode(), goalEvent.getScorerId()));
		}
		
		return details.toString();
	}
}
