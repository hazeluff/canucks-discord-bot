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

	}

	/*
	 * Convenient Instance Creators
	 */
	public static CustomGameMessage win(String message, Team team) {
		return new CustomGameMessage(
				message, 
				game -> {
					Team winningTeam = game.getWinningTeam();
					return winningTeam == null 
							? false
							: winningTeam.equals(team);
				}, 
				1);
	}

	public static CustomGameMessage lose(String message, Team team) {
		return new CustomGameMessage(
				message, 
				game -> {
					if (!game.containsTeam(team)) {
						return false;
					}
					Team winningTeam = game.getWinningTeam();
					return winningTeam == null 
							? false
							: !winningTeam.equals(team);
				}, 
				1);
	}

	public static CustomGameMessage shutout(String message, Team team) {
		return new CustomGameMessage(
				message, 
				game -> {
					Team winningTeam = game.getWinningTeam();
					if (winningTeam == null) {
						return false;
					}
					if (!winningTeam.equals(team)) {
						return false;
					}
					if(winningTeam.equals(game.getAwayTeam()) && game.getHomeScore() == 0) {
						return true;
					}
					if(winningTeam.equals(game.getHomeTeam()) && game.getAwayScore() == 0) {
						return true;
					}					
					return false;
				}, 
				2);
	}
}
