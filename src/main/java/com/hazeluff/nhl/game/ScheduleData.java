package com.hazeluff.nhl.game;

import java.time.ZonedDateTime;

import org.bson.BsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.utils.DateUtils;
import com.hazeluff.nhl.Team;

public class ScheduleData {
	private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleData.class);

	private BsonDocument rawJson;

	private final GameType gameType;
	private final ZonedDateTime startTime;
	private final int gameId;
	private final Team awayTeam;
	private final Team homeTeam;

	ScheduleData(BsonDocument rawJson, 
			GameType gameType, ZonedDateTime startTime, int gameId, 
			Team awayTeam, Team homeTeam) {
		this.rawJson = rawJson;
		this.gameType = gameType;
		this.startTime = startTime;
		this.gameId = gameId;
		this.awayTeam = awayTeam;
		this.homeTeam = homeTeam;
	}

	public static ScheduleData parse(BsonDocument rawScheduleGameJson) {
		try {
			GameType gameType = GameType.parse(rawScheduleGameJson.getInt32("gameType").getValue());
			ZonedDateTime date = DateUtils.parseNHLDate(rawScheduleGameJson.getString("startTimeUTC").getValue());
			int gameId = rawScheduleGameJson.getInt32("id").getValue();
			Team awayTeam = Team.parse(rawScheduleGameJson.getDocument("awayTeam").getInt32("id").getValue());
			Team homeTeam = Team.parse(rawScheduleGameJson.getDocument("homeTeam").getInt32("id").getValue());
			return new ScheduleData(
					rawScheduleGameJson, 
					gameType, date, gameId, 
					awayTeam, homeTeam
			);
		} catch (Exception e) {
			LOGGER.error("Could not parse json.", e);
			return null;
		}
	}
	
	public void update(BsonDocument rawScheduleGameJson) {
		this.rawJson = rawScheduleGameJson;
	}

	public String getGameScheduleState() {
		return rawJson.getString("gameScheduleState").getValue();
	}

	public GameState getGameState() {
		return GameState.parse(rawJson.getString("gameState").getValue());
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
