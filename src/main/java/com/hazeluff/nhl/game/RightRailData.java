package com.hazeluff.nhl.game;

import java.util.concurrent.atomic.AtomicReference;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RightRailData {
	private static final Logger LOGGER = LoggerFactory.getLogger(RightRailData.class);

	private AtomicReference<BsonDocument> jsonRR;

	private final TeamGameStats teamGameStats;

	RightRailData(BsonDocument rrJson, TeamGameStats teamGameStats) {
		this.jsonRR = new AtomicReference<>(rrJson);
		this.teamGameStats = teamGameStats;
	}

	public static RightRailData parse(BsonDocument rrJson) {
		try {
			TeamGameStats teamGameStats = TeamGameStats.parse(rrJson.getArray("teamGameStats", new BsonArray()));
			return new RightRailData(rrJson, teamGameStats);
		} catch (Exception e) {
			LOGGER.error("Could not parse json.", e);
			return null;
		}
	}

	public TeamGameStats getTeamGameStats() {
		return teamGameStats;
	}

	public void update(BsonDocument rrJson) {
		this.jsonRR.set(rrJson);
		this.teamGameStats.update(rrJson.getArray("teamGameStats", new BsonArray()));
	}

	public BsonDocument getJson() {
		return this.jsonRR.get();
	}

}
