package com.hazeluff.nhl.game;

import org.bson.BsonDocument;
import org.bson.BsonString;

public class LineScore {
	// Period
	private final int currentPeriod;
	private final String currentPeriodOrdinal;
	private final String currentPeriodTimeRemaining;
	private final boolean hasShootout;
	// Intermission
	private final boolean inIntermission;
	private final int intermissionTimeElapsed;
	private final int intermissionTimeRemaining;
	// Power Play
	private final String powerPlayStrength;
	private final Boolean inPowerPlay;
	private final Integer penaltyTimeElapsed;
	private final Integer penaltyTimeRemaining;
	// Score
	private final int homeGoals;
	private final Integer homeShotsOnGoal;
	private final int awayGoals;
	private final Integer awayShotsOnGoal;

	private LineScore(int currentPeriod, String currentPeriodOrdinal, String currentPeriodTimeRemaining,
			boolean hasShootout, boolean inIntermission, int intermissionTimeElapsed, int intermissionTimeRemaining,
			String powerPlayStrength, Boolean inPowerPlay, Integer penaltyTimeElapsed, Integer penaltyTimeRemaining,
			int homeGoals, Integer homeShotsOnGoal, int awayGoals, Integer awayShotsOnGoal) {
		this.currentPeriod = currentPeriod;
		this.currentPeriodOrdinal = currentPeriodOrdinal;
		this.currentPeriodTimeRemaining = currentPeriodTimeRemaining;
		this.hasShootout = hasShootout;
		this.inIntermission = inIntermission;
		this.intermissionTimeElapsed = intermissionTimeElapsed;
		this.intermissionTimeRemaining = intermissionTimeRemaining;
		this.powerPlayStrength = powerPlayStrength;
		this.inPowerPlay = inPowerPlay;
		this.penaltyTimeElapsed = penaltyTimeElapsed;
		this.penaltyTimeRemaining = penaltyTimeRemaining;
		this.homeGoals = homeGoals;
		this.homeShotsOnGoal = homeShotsOnGoal;
		this.awayGoals = awayGoals;
		this.awayShotsOnGoal = awayShotsOnGoal;
	}

	public static LineScore parse(BsonDocument json) {
		// Period
		int currentPeriod = json.getInt32("currentPeriod").getValue();
		String currentPeriodOrdinal = json.getString("currentPeriodOrdinal", new BsonString("")).getValue();
		String currentPeriodTimeRemaining = json.getString("currentPeriodTimeRemaining", new BsonString("")).getValue();
		boolean hasShootout = json.getBoolean("hasShootout").getValue();
		// Intermission
		BsonDocument jsonIntermission = json.getDocument("intermissionInfo");
		boolean inIntermission = jsonIntermission.getBoolean("inIntermission").getValue();
		int intermissionTimeElapsed = jsonIntermission.getInt32("intermissionTimeElapsed").getValue();
		int intermissionTimeRemaining = jsonIntermission.getInt32("intermissionTimeRemaining").getValue();
		// Powerplay
		String powerPlayStrength = json.getString("powerPlayStrength").getValue();
		BsonDocument jsonPowerPlay = json.getDocument("powerPlayInfo", null);
		Boolean inPowerPlay = jsonPowerPlay == null 
				? null 
				: jsonPowerPlay.getBoolean("inSituation").getValue();
		Integer penaltyTimeElapsed = jsonPowerPlay == null 
				? null 
				: jsonPowerPlay.getInt32("situationTimeElapsed").getValue();
		Integer penaltyTimeRemaining = jsonPowerPlay == null 
				? null 
				: jsonPowerPlay.getInt32("situationTimeRemaining").getValue();
		// Score
		BsonDocument jsonTeams = json.getDocument("teams");
		BsonDocument jsonHomeTeam = jsonTeams.getDocument("home");
		int homeGoals = jsonHomeTeam.getInt32("goals").getValue();
		Integer homeShotsOnGoal = jsonHomeTeam.containsKey("shotsOnGoal")
				? jsonHomeTeam.getInt32("shotsOnGoal").getValue()
				: null;
		BsonDocument jsonAwayTeam = jsonTeams.getDocument("away");
		int awayGoals = jsonAwayTeam.getInt32("goals").getValue();
		Integer awayShotsOnGoal = jsonAwayTeam.containsKey("shotsOnGoal")
				? jsonHomeTeam.getInt32("shotsOnGoal").getValue()
				: null;
		
		return new LineScore(currentPeriod, currentPeriodOrdinal, currentPeriodTimeRemaining, hasShootout,
				inIntermission, intermissionTimeElapsed, intermissionTimeRemaining, powerPlayStrength, inPowerPlay,
				penaltyTimeElapsed, penaltyTimeRemaining, homeGoals, homeShotsOnGoal, awayGoals, awayShotsOnGoal);
	}

	public int getCurrentPeriod() {
		return currentPeriod;
	}

	public String getCurrentPeriodOrdinal() {
		return currentPeriodOrdinal;
	}

	public String getCurrentPeriodTimeRemaining() {
		return currentPeriodTimeRemaining;
	}

	public boolean hasShootout() {
		return hasShootout;
	}

	public boolean isIntermission() {
		return inIntermission;
	}

	public int getIntermissionTimeElapsed() {
		return intermissionTimeElapsed;
	}

	public int getIntermissionTimeRemaining() {
		return intermissionTimeRemaining;
	}

	public String getPowerPlayStrength() {
		return powerPlayStrength;
	}

	public Boolean isPowerPlay() {
		return inPowerPlay;
	}

	public Integer getPenaltyTimeElapsed() {
		return penaltyTimeElapsed;
	}

	public Integer getPenaltyTimeRemaining() {
		return penaltyTimeRemaining;
	}

	public int getHomeScore() {
		return homeGoals;
	}

	public Integer getHomeShotsOnGoal() {
		return homeShotsOnGoal;
	}

	public int getAwayScore() {
		return awayGoals;
	}

	public Integer getAwayShotsOnGoal() {
		return awayShotsOnGoal;
	}

	@Override
	public String toString() {
		return "LineScore [currentPeriod=" + currentPeriod + ", currentPeriodOrdinal=" + currentPeriodOrdinal
				+ ", currentPeriodTimeRemaining=" + currentPeriodTimeRemaining + ", hasShootout=" + hasShootout
				+ ", inIntermission=" + inIntermission + ", intermissionTimeElapsed=" + intermissionTimeElapsed
				+ ", intermissionTimeRemaining=" + intermissionTimeRemaining + ", powerPlayStrength="
				+ powerPlayStrength + ", inPowerPlay=" + inPowerPlay + ", penaltyTimeElapsed=" + penaltyTimeElapsed
				+ ", penaltyTimeRemaining=" + penaltyTimeRemaining + ", homeGoals=" + homeGoals + ", homeShotsOnGoal="
				+ homeShotsOnGoal + ", awayGoals=" + awayGoals + ", awayShotsOnGoal=" + awayShotsOnGoal + "]";
	}
}
