package com.hazeluff.nhl;

import org.bson.BsonDocument;
import org.bson.BsonInt32;

import com.hazeluff.discord.nhl.NHLTeams.Team;

public class PlayoffSeries {
	private final BsonDocument jsonDoc;
	private final String seriesLetter;
	private final String seriesAbbrev;

	private PlayoffSeries(BsonDocument document) {
		this.jsonDoc = document;
		this.seriesLetter = document.getString("seriesLetter").getValue();
		this.seriesAbbrev = document.getString("seriesAbbrev").getValue();
	}

	public static PlayoffSeries parse(BsonDocument document) {
		return new PlayoffSeries(document);
	}

	public String getSeriesLetter() {
		return seriesLetter;
	}

	public String getSeriesAbbrev() {
		return seriesAbbrev;
	}

	public int getPlayoffRound() {
		return jsonDoc.getInt32("playoffRound").getValue();
	}

	public boolean isParticipantsSet() {
		return isTopSeedDetermined() && isBottomSeedDetermined();
	}

	public boolean hasParticipant() {
		return isTopSeedDetermined() || isBottomSeedDetermined();
	}

	public boolean isTopSeedDetermined() {
		return jsonDoc.containsKey("topSeedTeam")
			&& jsonDoc.getDocument("topSeedTeam").getInt32("id", new BsonInt32(-1)).getValue() > 0;
	}

	public boolean isBottomSeedDetermined() {
		return jsonDoc.containsKey("bottomSeedTeam")
			&& jsonDoc.getDocument("bottomSeedTeam").getInt32("id", new BsonInt32(-1)).getValue() > 0;
	}

	public Team getTopSeedTeam() {
		if (!jsonDoc.containsKey("topSeedTeam")) {
			return null;
		}

		return Team.parse(jsonDoc.getDocument("topSeedTeam").getInt32("id", new BsonInt32(-1)).getValue());
	}

	public Team getBottomSeedTeam() {
		if (!jsonDoc.containsKey("bottomSeedTeam")) {
			return null;
		}

		return Team.parse(jsonDoc.getDocument("bottomSeedTeam").getInt32("id", new BsonInt32(-1)).getValue());
	}

	public int getNextGameNumber() {
		return getTopSeedWins() + getBottomSeedWins() + 1;
	}

	public int getTopSeedWins() {
		return jsonDoc.getInt32("topSeedWins").getValue();
	}

	public int getBottomSeedWins() {
		return jsonDoc.getInt32("bottomSeedWins").getValue();
	}

	public boolean hasWinningTeam() {
		return jsonDoc.containsKey("winningTeamId");
	}

	public Team getWinningTeam() {
		if (!jsonDoc.containsKey("winningTeamId"))
			return null;
		return Team.parse(jsonDoc.getInt32("winningTeamId").getValue());
	}
}
