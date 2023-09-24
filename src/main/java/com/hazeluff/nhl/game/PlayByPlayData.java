package com.hazeluff.nhl.game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	private BsonDocument pbpJson;

	private final TeamStats homeStats;
	private final TeamStats awayStats;

	private final Map<Integer, RosterPlayer> players;

	PlayByPlayData(BsonDocument pbpJson,
			TeamStats homeStats, TeamStats awayStats,
			Map<Integer, RosterPlayer> players) {
		this.pbpJson = pbpJson;
		this.homeStats = homeStats;
		this.awayStats = awayStats;
		this.players = players;
	}

	public static PlayByPlayData parse(BsonDocument pbpJson) {
		try {
			TeamStats homeStats = TeamStats.parse(pbpJson.getDocument("homeTeam"));
			TeamStats awayStats = TeamStats.parse(pbpJson.getDocument("awayTeam"));
			Map<Integer, RosterPlayer> players = parseRosterSpots(pbpJson.getArray("rosterSpots"));
			return new PlayByPlayData(
					pbpJson,
					homeStats, awayStats, 
					players);
		} catch (Exception e) {
			LOGGER.error("Could not parse json.", e);
			return null;
		}
	}

	private static Map<Integer, RosterPlayer> parseRosterSpots(BsonArray array) {
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
		this.pbpJson = playByPlayJson;
		this.homeStats.update(playByPlayJson.getDocument("homeTeam"));
		this.homeStats.update(playByPlayJson.getDocument("awayTeam"));
		// this.players - Player rosters do not need to be updated
	}

	public String getGameScheduleState() {
		return pbpJson.getString("gameScheduleState").getValue();
	}

	public GameState getGameState() {
		return GameState.parse(pbpJson.getString("gameState").getValue());
	}

	public int getPeriod() {
		return pbpJson.getInt32("period").getValue();
	}

	public BsonDocument getClock() {
		return pbpJson.getDocument("clock");
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
		BsonDocument rawTeamStats;

		TeamStats(BsonDocument rawTeamStats) {
			this.rawTeamStats = rawTeamStats;
		}

		static TeamStats parse(BsonDocument rawTeamStats) {
			return new TeamStats(rawTeamStats);
		}

		void update(BsonDocument rawTeamStats) {
			this.rawTeamStats = rawTeamStats;
		}

		public int getScore() {
			return rawTeamStats.getInt32("score").getValue();
		}

		public int getSOG() {
			return rawTeamStats.getInt32("sog").getValue();
		}
	}

	public List<GameEvent> getPlays() {
		return pbpJson.getArray("plays")
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
