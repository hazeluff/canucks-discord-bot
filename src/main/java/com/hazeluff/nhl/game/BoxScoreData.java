package com.hazeluff.nhl.game;

import java.util.concurrent.atomic.AtomicReference;

import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoxScoreData {
	private static final Logger LOGGER = LoggerFactory.getLogger(BoxScoreData.class);

	private AtomicReference<BsonDocument> jsonBoxScore;

	private final TeamStats homeStats;
	private final TeamStats awayStats;

	BoxScoreData(BsonDocument jsonBoxScore, TeamStats homeStats, TeamStats awayStats) {
		this.jsonBoxScore = new AtomicReference<>(jsonBoxScore);
		this.homeStats = homeStats;
		this.awayStats = awayStats;
	}

	public static BoxScoreData parse(BsonDocument jsonPbp) {
		try {
			TeamStats homeStats = TeamStats.parse(jsonPbp.getDocument("homeTeam"));
			TeamStats awayStats = TeamStats.parse(jsonPbp.getDocument("awayTeam"));
			return new BoxScoreData(jsonPbp, homeStats, awayStats);
		} catch (Exception e) {
			LOGGER.error("Could not parse json.", e);
			return null;
		}
	}

	public void update(BsonDocument playByPlayJson) {
		this.jsonBoxScore.set(playByPlayJson);
		this.homeStats.update(playByPlayJson.getDocument("homeTeam"));
		this.awayStats.update(playByPlayJson.getDocument("awayTeam"));
	}

	public BsonDocument getJson() {
		return jsonBoxScore.get();
	}

	public String getGameScheduleState() {
		return getJson().getString("gameScheduleState").getValue();
	}

	public GameState getGameState() {
		return GameState.parse(getJson().getString("gameState").getValue());
	}

	public int getPeriod() {
		return getJson().getInt32("period", new BsonInt32(0)).getValue();
	}

	public BsonDocument getClock() {
		return getJson().getDocument("clock");
	}

	public boolean isInIntermission() {
		return getClock().getBoolean("inIntermission").getValue();
	}

	public String getClockRemaining() {
		return getClock().getString("timeRemaining").getValue();
	}

	public TeamStats getHomeStats() {
		return homeStats;
	}

	public TeamStats getAwayStats() {
		return awayStats;
	}

	public static class TeamStats {
		AtomicReference<BsonDocument> jsonTeamStats;

		TeamStats(BsonDocument jsonTeamStats) {
			this.jsonTeamStats = new AtomicReference<>(jsonTeamStats);
		}

		static TeamStats parse(BsonDocument jsonTeamStats) {
			return new TeamStats(jsonTeamStats);
		}

		public BsonDocument getJson() {
			return this.jsonTeamStats.get();
		}

		void update(BsonDocument jsonTeamStats) {
			this.jsonTeamStats.set(jsonTeamStats);
		}

		public int getBlocks() {
			return getJson().getInt32("blocks").getValue();
		}

		public double getFaceoffWinPctg() {
			return getJson().getDouble("faceoffWinningPctg").getValue();
		}

		public int getHits() {
			return getJson().getInt32("hits").getValue();
		}

		public String getPowerPlayConv() {
			return getJson().getString("powerPlayConversion").getValue();
		}

		public int getPim() {
			return getJson().getInt32("pim").getValue();
		}

		public int getScore() {
			return getJson().getInt32("score").getValue();
		}

		public int getSog() {
			return getJson().getInt32("sog").getValue();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((jsonTeamStats == null) ? 0 : jsonTeamStats.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TeamStats other = (TeamStats) obj;
			if (jsonTeamStats == null) {
				if (other.jsonTeamStats != null)
					return false;
			} else if (!jsonTeamStats.equals(other.jsonTeamStats))
				return false;
			return true;
		}

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((awayStats == null) ? 0 : awayStats.hashCode());
		result = prime * result + ((homeStats == null) ? 0 : homeStats.hashCode());
		result = prime * result + ((jsonBoxScore == null) ? 0 : jsonBoxScore.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BoxScoreData other = (BoxScoreData) obj;
		if (awayStats == null) {
			if (other.awayStats != null)
				return false;
		} else if (!awayStats.equals(other.awayStats))
			return false;
		if (homeStats == null) {
			if (other.homeStats != null)
				return false;
		} else if (!homeStats.equals(other.homeStats))
			return false;
		if (jsonBoxScore == null) {
			if (other.jsonBoxScore != null)
				return false;
		} else if (!jsonBoxScore.equals(other.jsonBoxScore))
			return false;
		return true;
	}
}
