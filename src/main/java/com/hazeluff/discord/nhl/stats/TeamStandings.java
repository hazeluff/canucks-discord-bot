package com.hazeluff.discord.nhl.stats;

import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;

import com.hazeluff.nhl.Team;

public class TeamStandings {
	private final Team team;

	private final int gamesPlayed;
	private final int wins;
	private final int losses;
	private final int otLosses;
	private final int points;
	private final int goalFor;
	private final int goalAgainst;
	private final int goalDifferential;

	private final String clinchIndicator;
	private final int wildcardSequence;

	private final String conferenceAbbrev;
	private final String conferenceName;
	private final int conferenceSequence;

	private final String divisionAbbrev;
	private final String divisionName;
	private final int divisionSequence;

	private final int homeWins;
	private final int homeLosses;
	private final int homeOtLosses;

	private final int roadWins;
	private final int roadLosses;
	private final int roadOtLosses;

	private final int l10Wins;
	private final int l10Losses;
	private final int l10OtLosses;

	private final String streakCode;
	private final int streakCount;

	private final int waiversSequence;

	public TeamStandings(
			Team team, 
			int gamesPlayed, int wins, int losses, int otLosses, int points,
			int goalFor, int goalAgainst, int goalDifferential, 
			String clinchIndicator, int wildcardSequence, 
			String conferenceAbbrev, String conferenceName, int conferenceSequence, 
			String divisionAbbrev, String divisionName, int divisionSequence, 
			int homeWins, int homeLosses, int homeOtLosses, 
			int roadWins, int roadLosses, int roadOtLosses,
			int l10Wins, int l10Losses, int l10OtLosses, 
			String streakCode, int streakCount,
			int waiversSequence) {
		this.team = team;
		this.gamesPlayed = gamesPlayed;
		this.wins = wins;
		this.losses = losses;
		this.otLosses = otLosses;
		this.points = points;
		this.goalFor = goalFor;
		this.goalAgainst = goalAgainst;
		this.goalDifferential = goalDifferential;
		this.clinchIndicator = clinchIndicator;
		this.wildcardSequence = wildcardSequence;
		this.conferenceAbbrev = conferenceAbbrev;
		this.conferenceName = conferenceName;
		this.conferenceSequence = conferenceSequence;
		this.divisionAbbrev = divisionAbbrev;
		this.divisionName = divisionName;
		this.divisionSequence = divisionSequence;
		this.homeWins = homeWins;
		this.homeLosses = homeLosses;
		this.homeOtLosses = homeOtLosses;
		this.roadWins = roadWins;
		this.roadLosses = roadLosses;
		this.roadOtLosses = roadOtLosses;
		this.l10Wins = l10Wins;
		this.l10Losses = l10Losses;
		this.l10OtLosses = l10OtLosses;
		this.streakCode = streakCode;
		this.streakCount = streakCount;
		this.waiversSequence = waiversSequence;
	}

	public static TeamStandings parse(BsonDocument jsonStandings) {
		Team team = Team.parse(jsonStandings.getDocument("teamAbbrev").getString("default").getValue());

		int gamesPlayed = jsonStandings.getInt32("gamesPlayed").getValue();
		int wins = jsonStandings.getInt32("wins").getValue();
		int losses = jsonStandings.getInt32("losses").getValue();
		int otLosses = jsonStandings.getInt32("otLosses").getValue();
		int points = jsonStandings.getInt32("points").getValue();

		int goalFor = jsonStandings.getInt32("goalFor").getValue();
		int goalAgainst = jsonStandings.getInt32("goalAgainst").getValue();
		int goalDifferential = jsonStandings.getInt32("goalDifferential").getValue();

		String clinchIndicator = jsonStandings.getString("clinchIndicator", new BsonString("")).getValue();
		int wildcardSequence = jsonStandings.getInt32("wildcardSequence").getValue();

		String conferenceAbbrev = jsonStandings.getString("conferenceAbbrev").getValue();
		String conferenceName = jsonStandings.getString("conferenceName").getValue();
		int conferenceSequence = jsonStandings.getInt32("conferenceSequence").getValue();

		String divisionAbbrev = jsonStandings.getString("divisionAbbrev").getValue();
		String divisionName = jsonStandings.getString("divisionName").getValue();
		int divisionSequence = jsonStandings.getInt32("divisionSequence").getValue();

		int homeWins = jsonStandings.getInt32("homeWins").getValue();
		int homeLosses = jsonStandings.getInt32("homeLosses").getValue();
		int homeOtLosses = jsonStandings.getInt32("homeOtLosses").getValue();

		int roadWins = jsonStandings.getInt32("roadWins").getValue();
		int roadLosses = jsonStandings.getInt32("roadLosses").getValue();
		int roadOtLosses = jsonStandings.getInt32("roadOtLosses").getValue();

		int l10Wins = jsonStandings.getInt32("l10Wins").getValue();
		int l10Losses = jsonStandings.getInt32("l10Losses").getValue();
		int l10OtLosses = jsonStandings.getInt32("l10OtLosses").getValue();

		String streakCode = jsonStandings.getString("streakCode", new BsonString("")).getValue();
		int streakCount = jsonStandings.getInt32("streakCount", new BsonInt32(0)).getValue();

		int waiversSequence = jsonStandings.getInt32("waiversSequence").getValue();
		return new TeamStandings(
				team,
				gamesPlayed, wins, losses, otLosses, points,
				goalFor, goalAgainst, goalDifferential,
				clinchIndicator, wildcardSequence,
				conferenceAbbrev, conferenceName, conferenceSequence,
				divisionAbbrev, divisionName, divisionSequence,
				homeWins, homeLosses, homeOtLosses,
				roadWins, roadLosses, roadOtLosses,
				l10Wins, l10Losses, l10OtLosses,
				streakCode, streakCount,
				waiversSequence
		);
	}

	public Team getTeam() {
		return team;
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

	public int getOtLosses() {
		return otLosses;
	}

	public int getPoints() {
		return points;
	}

	public int getGoalFor() {
		return goalFor;
	}

	public int getGoalAgainst() {
		return goalAgainst;
	}

	public int getGoalDifferential() {
		return goalDifferential;
	}

	public String getClinchIndicator() {
		return clinchIndicator;
	}

	public int getWildcardSequence() {
		return wildcardSequence;
	}

	public String getConferenceAbbrev() {
		return conferenceAbbrev;
	}

	public String getConferenceName() {
		return conferenceName;
	}

	public int getConferenceSequence() {
		return conferenceSequence;
	}

	public String getDivisionAbbrev() {
		return divisionAbbrev;
	}

	public String getDivisionName() {
		return divisionName;
	}

	public int getDivisionSequence() {
		return divisionSequence;
	}

	public int getHomeWins() {
		return homeWins;
	}

	public int getHomeLosses() {
		return homeLosses;
	}

	public int getHomeOtLosses() {
		return homeOtLosses;
	}

	public int getRoadWins() {
		return roadWins;
	}

	public int getRoadLosses() {
		return roadLosses;
	}

	public int getRoadOtLosses() {
		return roadOtLosses;
	}

	public int getL10Wins() {
		return l10Wins;
	}

	public int getL10Losses() {
		return l10Losses;
	}

	public int getL10OtLosses() {
		return l10OtLosses;
	}

	public String getStreakCode() {
		return streakCode;
	}

	public int getStreakCount() {
		return streakCount;
	}

	public int getWaiversSequence() {
		return waiversSequence;
	}

}
