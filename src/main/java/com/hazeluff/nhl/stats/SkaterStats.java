package com.hazeluff.nhl.stats;

import org.bson.BsonDocument;

public class SkaterStats {
	private final int playerId;
	private final String firstName;
	private final String lastName;
	private final String positionCode;
	private final int gamesPlayed;
	private final int goals;
	private final int assists;
	private final int points;
	private final int plusMinus;
	private final int penaltyMinutes;
	private final int shots;
	private final double shootingPctg;
	private final int powerPlayGoals;
	private final int shorthandedGoals;
	private final int gameWinningGoals;
	private final double avgShiftsPerGame;
	private final double avgTimeOnIcePerGame;
	private final double faceoffWinPctg;

	public SkaterStats(
			int playerId, String firstName, String lastName, String positionCode, 
			int gamesPlayed, int goals, int assists, int points, 
			int plusMinus, int penaltyMinutes, int shots,
			double shootingPctg,
			int powerPlayGoals, int shorthandedGoals, int gameWinningGoals, 
			double avgShiftsPerGame, double avgTimeOnIcePerGame, double faceoffWinPctg) {
		this.playerId = playerId;
		this.firstName = firstName;
		this.lastName = lastName;
		this.positionCode = positionCode;
		this.gamesPlayed = gamesPlayed;
		this.goals = goals;
		this.assists = assists;
		this.points = points;
		this.plusMinus = plusMinus;
		this.penaltyMinutes = penaltyMinutes;
		this.shots = shots;
		this.shootingPctg = shootingPctg;
		this.powerPlayGoals = powerPlayGoals;
		this.shorthandedGoals = shorthandedGoals;
		this.gameWinningGoals = gameWinningGoals;
		this.avgShiftsPerGame = avgShiftsPerGame;
		this.avgTimeOnIcePerGame = avgTimeOnIcePerGame;
		this.faceoffWinPctg = faceoffWinPctg;
	}
	
	public static SkaterStats parse(BsonDocument jsonStats) {
		int playerId = jsonStats.getInt32("playerId").getValue();
		String firstName = jsonStats.getDocument("firstName").getString("default").getValue();
		String lastName = jsonStats.getDocument("lastName").getString("default").getValue();
		String positionCode = jsonStats.getString("positionCode").getValue();

		int gamesPlayed = jsonStats.getInt32("gamesPlayed").getValue();
		int goals = jsonStats.getInt32("goals").getValue();
		int assists = jsonStats.getInt32("assists").getValue();
		int points = jsonStats.getInt32("points").getValue();
		int plusMinus = jsonStats.getInt32("plusMinus").getValue();

		int penaltyMinutes = jsonStats.getInt32("penaltyMinutes").getValue();
		int shots = jsonStats.getInt32("shots").getValue();
		double shootingPctg = jsonStats.getDouble("shootingPctg").getValue();

		int powerPlayGoals = jsonStats.getInt32("powerPlayGoals").getValue();
		int shorthandedGoals = jsonStats.getInt32("shorthandedGoals").getValue();
		int gameWinningGoals = jsonStats.getInt32("gameWinningGoals").getValue();

		double avgShiftsPerGame = jsonStats.getDouble("avgShiftsPerGame").getValue();
		double avgTimeOnIcePerGame = jsonStats.getDouble("avgTimeOnIcePerGame").getValue();
		double faceoffWinPctg = jsonStats.getDouble("faceoffWinPctg").getValue();

		return new SkaterStats(
				playerId, firstName, lastName, positionCode, 
				gamesPlayed, goals, assists, points, 
				plusMinus, penaltyMinutes, shots, shootingPctg, 
				powerPlayGoals, shorthandedGoals, gameWinningGoals, 
				avgShiftsPerGame, avgTimeOnIcePerGame, faceoffWinPctg);
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

	public String getPositionCode() {
		return positionCode;
	}

	public int getGamesPlayed() {
		return gamesPlayed;
	}

	public int getGoals() {
		return goals;
	}

	public int getAssists() {
		return assists;
	}

	public int getPoints() {
		return points;
	}

	public int getPlusMinus() {
		return plusMinus;
	}

	public int getPenaltyMinutes() {
		return penaltyMinutes;
	}

	public int getShots() {
		return shots;
	}

	public double getShootingPctg() {
		return shootingPctg;
	}

	public int getPowerPlayGoals() {
		return powerPlayGoals;
	}

	public int getShorthandedGoals() {
		return shorthandedGoals;
	}

	public int getGameWinningGoals() {
		return gameWinningGoals;
	}

	public double getAvgShiftsPerGame() {
		return avgShiftsPerGame;
	}

	public double getAvgTimeOnIcePerGame() {
		return avgTimeOnIcePerGame;
	}

	public double getFaceoffWinPctg() {
		return faceoffWinPctg;
	}

	public boolean isForward() {
		switch (positionCode) {
		case "C":
		case "L":
		case "R":
			return true;
		default:
			return false;
		}
	}

	public boolean isDefender() {
		switch (positionCode) {
		case "D":
			return true;
		default:
			return false;
		}
	}

	@Override
	public String toString() {
		return "SkaterStats ["
					+ "playerId=" + playerId
					+ ", firstName=" + firstName
					+ ", lastName=" + lastName
					+ ", positionCode=" + positionCode
					+ ", gamesPlayed=" + gamesPlayed
					+ ", goals=" + goals
					+ ", assists="+ assists
					+ ", plusMinus=" + plusMinus
					+ ", penaltyMinutes=" + penaltyMinutes
					+ ", shots=" + shots
					+ ", shootingPctg=" + shootingPctg
					+ ", powerPlayGoals=" + powerPlayGoals
					+ ", shorthandedGoals="+ shorthandedGoals
					+ ", gameWinningGoals=" + gameWinningGoals
					+ ", avgShiftsPerGame=" + avgShiftsPerGame
					+ ", avgTimeOnIcePerGame=" + avgTimeOnIcePerGame
					+ ", faceoffWinPctg=" + faceoffWinPctg
				+ "]";
	}
	
}
