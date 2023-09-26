package com.hazeluff.nhl.game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.nhl.Team;
import com.hazeluff.nhl.event.GameEvent;

public class PlayByPlayData {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlayByPlayData.class);

	private AtomicReference<BsonDocument> jsonPbp;

	private final TeamStats homeStats;
	private final TeamStats awayStats;

	private final Map<Integer, RosterPlayer> players;

	PlayByPlayData(BsonDocument pbpJson,
			TeamStats homeStats, TeamStats awayStats,
			Map<Integer, RosterPlayer> players) {
		this.jsonPbp = new AtomicReference<>(pbpJson);
		this.homeStats = homeStats;
		this.awayStats = awayStats;
		this.players = players;
	}

	public static PlayByPlayData parse(BsonDocument jsonPbp) {
		try {
			TeamStats homeStats = TeamStats.parse(jsonPbp.getDocument("homeTeam"));
			TeamStats awayStats = TeamStats.parse(jsonPbp.getDocument("awayTeam"));
			Map<Integer, RosterPlayer> players = parseRosterSpots(jsonPbp.getArray("rosterSpots"));
			return new PlayByPlayData(
					jsonPbp,
					homeStats, awayStats, 
					players);
		} catch (Exception e) {
			LOGGER.error("Could not parse json.", e);
			return null;
		}
	}

	static Map<Integer, RosterPlayer> parseRosterSpots(BsonArray array) {
		Map<Integer, RosterPlayer> players = new HashMap<>();
		for (BsonValue arrVal : array) {
			BsonDocument player = arrVal.asDocument();
			RosterPlayer rosterPlayer = new RosterPlayer(
					player.getInt32("playerId").getValue(),
					player.getString("firstName").getValue(),
					player.getString("lastName").getValue(),
					player.getString("positionCode").getValue(),
					player.getInt32("sweaterNumber").getValue(), 
					Team.parse(player.getInt32("teamId").getValue())
			);
			players.put(rosterPlayer.getPlayerId(), rosterPlayer);
		}
		return players;
	}

	public void update(BsonDocument playByPlayJson) {
		this.jsonPbp.set(playByPlayJson);
		this.homeStats.update(playByPlayJson.getDocument("homeTeam"));
		this.awayStats.update(playByPlayJson.getDocument("awayTeam"));
		// this.players - Player rosters do not need to be updated
	}

	public BsonDocument getJson() {
		return jsonPbp.get();
	}

	public String getGameScheduleState() {
		return getJson().getString("gameScheduleState").getValue();
	}

	public GameState getGameState() {
		return GameState.parse(getJson().getString("gameState").getValue());
	}

	public int getPeriod() {
		return getJson().getInt32("period").getValue();
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

	static class TeamStats {
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

		public int getScore() {
			return getJson().getInt32("score").getValue();
		}

		public int getSOG() {
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

	public List<GameEvent> getPlays() {
		return getJson().getArray("plays")
			.getValues()
			.stream()
			.map(BsonValue::asDocument)
			.map(GameEvent::parse)
			.collect(Collectors.toList());
	}

	public Map<Integer, RosterPlayer> getPlayers() {
		return players;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((awayStats == null) ? 0 : awayStats.hashCode());
		result = prime * result + ((homeStats == null) ? 0 : homeStats.hashCode());
		result = prime * result + ((jsonPbp == null) ? 0 : jsonPbp.hashCode());
		result = prime * result + ((players == null) ? 0 : players.hashCode());
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
		PlayByPlayData other = (PlayByPlayData) obj;
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
		if (jsonPbp == null) {
			if (other.jsonPbp != null)
				return false;
		} else if (!jsonPbp.equals(other.jsonPbp))
			return false;
		if (players == null) {
			if (other.players != null)
				return false;
		} else if (!players.equals(other.players))
			return false;
		return true;
	}
}
