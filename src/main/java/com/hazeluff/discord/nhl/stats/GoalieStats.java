package com.hazeluff.discord.nhl.stats;

import org.bson.BsonDocument;

public class GoalieStats {
	private final int playerId;
	private final String firstName;
	private final String lastName;
	private final int gamesPlayed;
	private final int wins;
	private final int losses;
	private final int overtimeLosses;

	private final int saves;
	private final double savePercentage;
	private final double goalsAgainstAverage;
	private final int shotsAgainst;
	private final int goalsAgainst;

	private final int gamesStarted;
	private final int timeOnIce;

	private final int penaltyMinutes;
	private final int goals;
	private final int assists;

	public GoalieStats(
			int playerId, String firstName, String lastName, 
			int gamesPlayed, int wins, int losses, int overtimeLosses, 
			int saves, int shotsAgainst, int goalsAgainst, 
			double savePercentage,
			double goalsAgainstAverage, 
			int gamesStarted, int timeOnIce, 
			int penaltyMinutes, int goals, int assists) {
		this.playerId = playerId;
		this.firstName = firstName;
		this.lastName = lastName;
		this.gamesPlayed = gamesPlayed;
		this.wins = wins;
		this.losses = losses;
		this.overtimeLosses = overtimeLosses;
		this.saves = saves;
		this.shotsAgainst = shotsAgainst;
		this.goalsAgainst = goalsAgainst;
		this.savePercentage = savePercentage;
		this.goalsAgainstAverage = goalsAgainstAverage;
		this.gamesStarted = gamesStarted;
		this.timeOnIce = timeOnIce;
		this.penaltyMinutes = penaltyMinutes;
		this.goals = goals;
		this.assists = assists;
	}
	
	public static GoalieStats parse(BsonDocument jsonStats) {
		int playerId = jsonStats.getInt32("playerId").getValue();
		String firstName = jsonStats.getDocument("firstName").getString("default").getValue();
		String lastName = jsonStats.getDocument("lastName").getString("default").getValue();
		int gamesPlayed = jsonStats.getInt32("gamesPlayed").getValue();
		int wins = jsonStats.getInt32("wins").getValue();
		int losses = jsonStats.getInt32("losses").getValue();
		int overtimeLosses = jsonStats.getInt32("overtimeLosses").getValue();

		int saves = jsonStats.getInt32("saves").getValue();
		double savePercentage = jsonStats.getDouble("savePercentage").getValue();
		double goalsAgainstAverage = jsonStats.getDouble("goalsAgainstAverage").getValue();
		int shotsAgainst = jsonStats.getInt32("shotsAgainst").getValue();
		int goalsAgainst = jsonStats.getInt32("goalsAgainst").getValue();

		int gamesStarted = jsonStats.getInt32("gamesStarted").getValue();
		int timeOnIce = jsonStats.getInt32("timeOnIce").getValue();

		int penaltyMinutes = jsonStats.getInt32("penaltyMinutes").getValue();
		int goals = jsonStats.getInt32("goals").getValue();
		int assists = jsonStats.getInt32("assists").getValue();

		return new GoalieStats(
				playerId, firstName, lastName, 
				gamesPlayed, wins, losses, overtimeLosses, 
				saves, shotsAgainst, goalsAgainst, 
				savePercentage, goalsAgainstAverage, 
				gamesStarted, timeOnIce, penaltyMinutes,
				goals, assists);
	}

	public int getPlayerId() {
		return playerId;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public int getGamesPlayed() {
		return gamesPlayed;
	}

	public int getWins() {
		return wins;
	}

	public int getLosses() {
		return losses;
	}

	public int getOvertimeLosses() {
		return overtimeLosses;
	}

	public int getSaves() {
		return saves;
	}

	public int getShotsAgainst() {
		return shotsAgainst;
	}

	public int getGoalsAgainst() {
		return goalsAgainst;
	}

	public double getSavePercentage() {
		return savePercentage;
	}

	public double getGoalsAgainstAverage() {
		return goalsAgainstAverage;
	}

	public int getPenaltyMinutes() {
		return penaltyMinutes;
	}

	public int getGoals() {
		return goals;
	}

	public int getAssists() {
		return assists;
	}

	public int getGamesStarted() {
		return gamesStarted;
	}

	public int getTimeOnIce() {
		return timeOnIce;
	}

	@Override
	public String toString() {
		return "GoalieStats ["
					+ "playerId=" + playerId
					+ ", firstName=" + firstName
					+ ", lastName=" + lastName
					+ ", gamesPlayed=" + gamesPlayed
					+ ", wins=" + wins
					+ ", losses=" + losses
					+ ", overtimeLosses=" + overtimeLosses
					+ ", saves=" + saves
					+ ", savePercentage=" + savePercentage
					+ ", goalsAgainstAverage=" + goalsAgainstAverage
					+ ", shotsAgainst=" + shotsAgainst
					+ ", goalsAgainst=" + goalsAgainst
					+ ", gamesStarted=" + gamesStarted
					+ ", timeOnIce=" + timeOnIce
					+ ", penaltyMinutes=" + penaltyMinutes
					+ ", goals=" + goals
					+ ", assists=" + assists
				+ "]";
	}
	
	
}
