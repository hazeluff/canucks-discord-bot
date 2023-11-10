package com.hazeluff.nhl.game;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.BsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.gdc.GameDayChannel;
import com.hazeluff.nhl.Team;
import com.hazeluff.nhl.event.GameEvent;
import com.hazeluff.nhl.event.GoalEvent;
import com.hazeluff.nhl.event.PenaltyEvent;


public class Game {
	private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);

	private final ScheduleData scheduleData; // Set Once
	private PlayByPlayData pbpData; // Constantly Updated

	Game(ScheduleData scheduleInfo) {
		this.scheduleData = scheduleInfo;
		this.pbpData = null;
	}

	public static Game parse(BsonDocument jsonScheduleGame) {
		ScheduleData scheduleInfo = ScheduleData.parse(jsonScheduleGame);
		if (scheduleInfo != null) {
			return new Game(scheduleInfo);
		} else {
			LOGGER.error("Could not parse game: rawScheduleGameJson=" + jsonScheduleGame);
			return null;
		}
	}

	// Schedule
	public GameType getGameType() {
		return scheduleData.getGameType();
	}

	public String getPeriodOridnal() {
		return getGameType().getPeriodCode(getPeriod());
	}

	public ZonedDateTime getStartTime() {
		return scheduleData.getStartTime();
	}

	public int getGameId() {
		return scheduleData.getGameId();
	}

	public Team getAwayTeam() {
		return scheduleData.getAwayTeam();
	}

	public Team getHomeTeam() {
		return scheduleData.getHomeTeam();
	}

	/**
	 * Gets both home and aways teams as a list
	 * 
	 * @return list containing both home and away teams
	 */
	public List<Team> getTeams() {
		return Arrays.asList(getHomeTeam(), getAwayTeam());
	}

	/**
	 * Determines if the given team is participating in this game
	 * 
	 * @param team
	 * @return true, if team is a participant<br>
	 *         false, otherwise
	 */
	public boolean containsTeam(Team team) {
		return getHomeTeam().equals(team) || getAwayTeam().equals(team);
	}

	// PlayByPlay (Status)
	void initPlayByPlayInfo(BsonDocument jsonPlayByPlay) {
		PlayByPlayData newPbpInfo = PlayByPlayData.parse(jsonPlayByPlay);
		if (newPbpInfo != null) {
			this.pbpData = newPbpInfo;
		} else {
			LOGGER.error("Could not parse json: jsonPlayByPlay=" + jsonPlayByPlay);
		}
	}

	public void updatePlayByPlay(BsonDocument jsonPlayByPlay) {
		LOGGER.debug("Updating Game Boxscore Data. [" + getGameId() + "]");
		if (this.pbpData != null) {
			this.pbpData.update(jsonPlayByPlay);
		} else {
			initPlayByPlayInfo(jsonPlayByPlay);
		}
	}

	public GameState getGameState() {
		if (pbpData != null) {
			return this.pbpData.getGameState();
		}
		return this.scheduleData.getGameState();
	}

	public int getPeriod() {
		return this.pbpData.getPeriod();
	}

	public boolean isInIntermission() {
		return this.pbpData.isInIntermission();
	}

	public String getClockRemaining() {
		return this.pbpData.getClockRemaining();
	}

	public boolean hasShootout() {
		return getGameType().isShootout(getPeriod());
	}

	public int getHomeScore() {
		return this.pbpData.getHomeStats().getScore();
	}

	public int getAwayScore() {
		return this.pbpData.getAwayStats().getScore();
	}

	public Team getWinningTeam() {
		if (getHomeScore() > getAwayScore()) {
			return getHomeTeam();
		}
		if (getHomeScore() < getAwayScore()) {
			return getAwayTeam();
		}
		return null;
	}

	public RosterPlayer getPlayer(int playerId) {
		return pbpData.getPlayers().getOrDefault(playerId, null);
	}

	public List<GameEvent> getEvents() {
		return pbpData.getPlays();
	}

	public List<GoalEvent> getScoringEvents() {
		return getEvents().stream()
				.filter(event -> EventType.GOAL.equals(event.getType()))
				.map(GoalEvent.class::cast)
				.collect(Collectors.toList());
	}
	
	public List<PenaltyEvent> getPenaltyEvents() {
		return getEvents().stream()
				.filter(event -> EventType.PENALTY.equals(event.getType()))
				.map(PenaltyEvent.class::cast)
				.collect(Collectors.toList());
	}

	public boolean equals(Game other) {
		if (other == null) {
			return false;
		}
		return getGameId() == other.getGameId();
	}

	@Override
	public String toString() {
		return "Game [name()=" + GameDayChannel.getChannelName(this) + ", getGameState()=" + getGameState() + "]";
	}

}
