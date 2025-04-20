package com.hazeluff.discord.nhl;

import org.bson.BsonDocument;
import org.bson.BsonInt32;

import com.hazeluff.nhl.Team;

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
	
	public boolean hasParticipant() {
		return jsonDoc.containsKey("topSeedTeam") || jsonDoc.containsKey("bottomSeedTeam");
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

	public int getTopSeedWins() {
		return jsonDoc.getInt32("topSeedWins").getValue();
	}

	public int getBottomSeedWins() {
		return jsonDoc.getInt32("bottomSeedWins").getValue();
	}
}
