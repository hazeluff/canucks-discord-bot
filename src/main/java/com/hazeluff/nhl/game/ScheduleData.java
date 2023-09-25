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
	private final ZonedDateTime startTime;
	private final int gameId;
	private final Team awayTeam;
	private final Team homeTeam;

	ScheduleData(BsonDocument jsonSchedule, 
			GameType gameType, ZonedDateTime startTime, int gameId, 
			Team awayTeam, Team homeTeam) {
		this.jsonSchedule = new AtomicReference<BsonDocument>(jsonSchedule);
		this.gameType = gameType;
		this.startTime = startTime;
		this.gameId = gameId;
		this.awayTeam = awayTeam;
		this.homeTeam = homeTeam;
	}

	public static ScheduleData parse(BsonDocument jsonSchedule) {
		try {
			GameType gameType = GameType.parse(jsonSchedule.getInt32("gameType").getValue());
			ZonedDateTime date = DateUtils.parseNHLDate(jsonSchedule.getString("startTimeUTC").getValue());
			int gameId = jsonSchedule.getInt32("id").getValue();
			Team awayTeam = Team.parse(jsonSchedule.getDocument("awayTeam").getInt32("id").getValue());
			Team homeTeam = Team.parse(jsonSchedule.getDocument("homeTeam").getInt32("id").getValue());
			return new ScheduleData(
					jsonSchedule, 
					gameType, date, gameId, 
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
		return startTime;
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

}
