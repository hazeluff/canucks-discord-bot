package com.hazeluff.nhl.game;

import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicReference;

import org.bson.BsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.utils.DateUtils;
import com.hazeluff.nhl.Team;

public class ScheduleData {
	private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleData.class);

	private AtomicReference<BsonDocument> jsonSchedule;

	private final GameType gameType;
	private final int gameId;
	private final Team awayTeam;
	private final Team homeTeam;

	ScheduleData(BsonDocument jsonSchedule,
			GameType gameType, int gameId,
			Team awayTeam, Team homeTeam) {
		this.jsonSchedule = new AtomicReference<BsonDocument>(jsonSchedule);
		this.gameType = gameType;
		this.gameId = gameId;
		this.awayTeam = awayTeam;
		this.homeTeam = homeTeam;
	}

	public static ScheduleData parse(BsonDocument jsonSchedule) {
		try {
			GameType gameType = GameType.parse(jsonSchedule.getInt32("gameType").getValue());
			int gameId = jsonSchedule.getInt32("id").getValue();
			Team awayTeam = Team.parse(jsonSchedule.getDocument("awayTeam").getInt32("id").getValue());
			Team homeTeam = Team.parse(jsonSchedule.getDocument("homeTeam").getInt32("id").getValue());
			return new ScheduleData(
					jsonSchedule,
					gameType, gameId,
					awayTeam, homeTeam
			);
		} catch (Exception e) {
			LOGGER.error("Could not parse json.", e);
			return null;
		}
	}
	
	public void update(BsonDocument rawScheduleGameJson) {
		this.jsonSchedule.set(rawScheduleGameJson);
	}

	public BsonDocument getJson() {
		return this.jsonSchedule.get();
	}

	public String getGameScheduleState() {
		return getJson().getString("gameScheduleState").getValue();
	}

	public GameState getGameState() {
		return GameState.parse(getJson().getString("gameState").getValue());
	}

	public GameType getGameType() {
		return gameType;
	}

	public ZonedDateTime getStartTime() {
		return DateUtils.parseNHLDate(getJson().getString("startTimeUTC").getValue());
	}

	public int getGameId() {
		return gameId;
	}

	public Team getAwayTeam() {
		return awayTeam;
	}

	public Team getHomeTeam() {
		return homeTeam;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((awayTeam == null) ? 0 : awayTeam.hashCode());
		result = prime * result + gameId;
		result = prime * result + ((gameType == null) ? 0 : gameType.hashCode());
		result = prime * result + ((homeTeam == null) ? 0 : homeTeam.hashCode());
		result = prime * result + ((jsonSchedule == null) ? 0 : jsonSchedule.hashCode());
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
		ScheduleData other = (ScheduleData) obj;
		if (awayTeam != other.awayTeam)
			return false;
		if (gameId != other.gameId)
			return false;
		if (gameType != other.gameType)
			return false;
		if (homeTeam != other.homeTeam)
			return false;
		if (jsonSchedule == null) {
			if (other.jsonSchedule != null)
				return false;
		} else if (!jsonSchedule.equals(other.jsonSchedule))
			return false;
		return true;
	}
}
