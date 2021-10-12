package com.hazeluff.discord.bot.command.gdc;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;

import com.hazeluff.nhl.Player;
import com.hazeluff.nhl.event.GameEvent;
import com.hazeluff.nhl.event.GoalEvent;
import com.hazeluff.nhl.game.Game;
import com.hazeluff.nhl.game.Status;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;

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
	public Publisher<?> reply(ChatInputInteractionEvent event, Game game) {
		if (!game.getStatus().isStarted()) {
			return event.reply(GAME_NOT_STARTED_MESSAGE);
		}

		return event.reply(callbackSpec -> callbackSpec
				.addEmbed(embedSpec -> GDCScoreCommand.buildEmbed(embedSpec, game))
				.addEmbed(embedSpec -> buildEmbed(embedSpec, game))
		);
	}

	public static EmbedCreateSpec buildEmbed(EmbedCreateSpec embedSpec, Game game) {
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

		Status status = game.getStatus();
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
