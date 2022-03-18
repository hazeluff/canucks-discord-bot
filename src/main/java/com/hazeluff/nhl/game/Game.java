package com.hazeluff.nhl.game;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.utils.DateUtils;
import com.hazeluff.nhl.Team;
import com.hazeluff.nhl.event.GameEvent;
import com.hazeluff.nhl.event.GoalEvent;
import com.hazeluff.nhl.event.PenaltyEvent;
import com.hazeluff.nhl.game.data.LiveData;
import com.hazeluff.nhl.game.data.LiveDataException;


public class Game {
	private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);

	private final ZonedDateTime date;
	private final int gamePk;
	private final Team awayTeam;
	private final Team homeTeam;
	
	private BsonDocument rawScheduleData;
	private LiveData liveData = null;

	Game(ZonedDateTime date, int gamePk, Team awayTeam, Team homeTeam, BsonDocument rawScheduleData) {
		this.date = date;
		this.gamePk = gamePk;
		this.awayTeam = awayTeam;
		this.homeTeam = homeTeam;

		this.rawScheduleData = rawScheduleData;
	}

	public static Game parse(BsonDocument rawScheduleGameJson) {
		try {
			ZonedDateTime date = DateUtils.parseNHLDate(rawScheduleGameJson.getString("gameDate").getValue());
			int gamePk = rawScheduleGameJson.getInt32("gamePk").getValue();
			Team awayTeam = Team.parse(rawScheduleGameJson.getDocument("teams").getDocument("away")
					.getDocument("team").getInt32("id").getValue());
			Team homeTeam = Team.parse(rawScheduleGameJson.getDocument("teams").getDocument("home")
					.getDocument("team").getInt32("id").getValue());
			Game game = new Game(date, gamePk, awayTeam, homeTeam, rawScheduleGameJson);
			return game;
		} catch (Exception e) {
			LOGGER.error("Could not parse game.", e);
			return null;
		}
	}

	public void updateGameData(BsonDocument rawScheduleData) {
		LOGGER.trace("Updating Game Schedule Data. [" + gamePk + "]");
		this.rawScheduleData = rawScheduleData;
	}

	public void updateLiveData() {
		LOGGER.debug("Updating Game Live Data. [" + gamePk + "]");
		if (liveData == null) {
			liveData = LiveData.create(getGamePk());
		} else {
			liveData.update();
		}
	}

	public void resetLiveData() throws LiveDataException {
		if (liveData != null) {
			liveData.resetLiveData();
		}
	}

	public BsonDocument getScheduledData() {
		return rawScheduleData;
	}

	public ZonedDateTime getDate() {
		return date;
	}

	public int getGamePk() {
		return gamePk;
	}

	public Team getAwayTeam() {
		return awayTeam;
	}

	public Team getHomeTeam() {
		return homeTeam;
	}

	public Team getWinningTeam() {
		if (!getStatus().isFinished()) {
			return null;
		}
		if (getLineScore().getHomeScore() > getLineScore().getAwayScore()) {
			return homeTeam;
		}
		if (getLineScore().getAwayScore() > getLineScore().getHomeScore()) {
			return awayTeam;
		}

		return null;
	}

	/**
	 * Gets both home and aways teams as a list
	 * 
	 * @return list containing both home and away teams
	 */
	public List<Team> getTeams() {
		return Arrays.asList(homeTeam, awayTeam);
	}

	/**
	 * Determines if the given team is participating in this game
	 * 
	 * @param team
	 * @return true, if team is a participant<br>
	 *         false, otherwise
	 */
	public boolean containsTeam(Team team) {
		return awayTeam == team || homeTeam == team;
	}

	public Status getStatus() {
		if (liveData != null) {
			return liveData.getStatus();
		}
		return Status.parse(getScheduledData().getDocument("status"));
	}

	public LineScore getLineScore() {
		return liveData == null ? null : liveData.getLinescore();
	}

	public int getHomeScore() {
		if (liveData != null) {
			return liveData.getLinescore().getHomeScore();
		}
		return getScheduledData().getDocument("teams").getDocument("home").getInt32("score").getValue();
	}

	public int getAwayScore() {
		if (liveData != null) {
			return liveData.getLinescore().getAwayScore();
		}
		return getScheduledData().getDocument("teams").getDocument("away").getInt32("score").getValue();
	}

	public List<GameEvent> getEvents() {
		if (liveData == null) {
			return Collections.emptyList();
		}
		return liveData.getPlays().getArray("allPlays")
				.getValues()
				.stream()
				.map(BsonValue::asDocument)
				.map(GameEvent::of)
				.collect(Collectors.toList());
	}

	public List<GoalEvent> getScoringEvents() {
		if (liveData == null) {
			return getScheduledData().getArray("scoringPlays")
					.stream()
					.map(jsonPlay -> jsonPlay.asDocument())
					.map(GameEvent::of)
					.map(GoalEvent.class::cast)
					.collect(Collectors.toList());
		}
		return getEvents().stream()
				.filter(GoalEvent.class::isInstance)
				.map(GoalEvent.class::cast)
				.collect(Collectors.toList());
	}
	
	public List<PenaltyEvent> getPenaltyEvents() {
		return getEvents().stream()
				.filter(PenaltyEvent.class::isInstance)
				.map(PenaltyEvent.class::cast)
				.collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return "Game [date=" + date + ", gamePk=" + gamePk + ", awayTeam=" + awayTeam + ", homeTeam=" + homeTeam
				+ ", rawScheduleData=" + rawScheduleData + ", liveData=" + liveData + "]";
	}

	public boolean equals(Game other) {
		return gamePk == other.gamePk;
	}
}
