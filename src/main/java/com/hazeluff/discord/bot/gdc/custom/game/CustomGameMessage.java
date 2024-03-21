package com.hazeluff.discord.bot.gdc.custom.game;

import java.util.ArrayList;
import java.util.function.Predicate;

import com.hazeluff.nhl.Team;
import com.hazeluff.nhl.game.Game;

public class CustomGameMessage {

	private final String message;
	private final Predicate<Game> gameApplicable;
	private final int priority;

	public CustomGameMessage(String message, Predicate<Game> gameApplicable, int priority) {
		this.message = message;
		this.gameApplicable = gameApplicable;
		this.priority = priority;
	}

	public String getMessage() {
		return message;
	}

	public int getPriority() {
		return priority;
	}

	public boolean applies(Game game) {
		return gameApplicable.test(game);
	}

	@SuppressWarnings("serial")
	public static abstract class Collection extends ArrayList<CustomGameMessage> {
		abstract Team getTeam();

		/*
		 * Convenient List Appenders
		 */
		public void win(String message) {
			CustomGameMessage customMessage = new CustomGameMessage(
					message, 
					buildWinConditions(getTeam()), 
					1);
			add(customMessage);
		}
		
		public void mostGoalsOrPoints(String message, int playerId) {
			CustomGameMessage customMessage = new CustomGameMessage(
					message, 
					buildMostGoalsOrPointsCondition(getTeam(), playerId), 
					2);
			add(customMessage);
		}
		
		public void lose(String message) {
			CustomGameMessage customMessage = new CustomGameMessage(
					message, 
					buildWinConditions(getTeam()).negate(), 
					1);
			add(customMessage);
		}
		
		public void shutout(String message) {
			CustomGameMessage customMessage = new CustomGameMessage(
					message, 
					buildShutoutConditions(getTeam()), 
					2);
			add(customMessage);
		}

		/*
		 * Predicates
		 */
		static Predicate<Game> buildWinConditions(Team team) {
			return game -> {
				Team winningTeam = game.getWinningTeam();
				return winningTeam == null 
						? false
						: winningTeam.equals(team);
			};
		}

		static Predicate<Game> buildShutoutConditions(Team team) {
			return game -> {
				Team winningTeam = game.getWinningTeam();
				if (winningTeam == null) {
					return false;
				}
				if (!winningTeam.equals(team)) {
					return false;
				}
				if (winningTeam.equals(game.getAwayTeam()) && game.getHomeScore() == 0) {
					return true;
				}
				if (winningTeam.equals(game.getHomeTeam()) && game.getAwayScore() == 0) {
					return true;
				}
				return false;
			};
		}

		static Predicate<Game> buildMostGoalsOrPointsCondition(Team team, int playerId) {
			return game -> {
				Team winningTeam = game.getWinningTeam();
				boolean isWin = winningTeam != null && winningTeam.equals(team);
				if (!isWin) {
					return false;
				}
				if (game.getTopGoalScorers().contains(playerId) || game.getTopPointScorers().contains(playerId)) {
					return true;
				}
				return false;
			};
		}
	}
}
