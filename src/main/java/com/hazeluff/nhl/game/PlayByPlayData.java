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
		this.homeStats.update(playByPlayJson.getDocument("awayTeam"));
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
}
