package com.hazeluff.nhl;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.utils.DateUtils;


public class Game {
	private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);

	private final ZonedDateTime date;
	private final int gamePk;
	private final Team awayTeam;
	private final Team homeTeam;
	
	private JSONObject rawGameData;
	private GameLiveData gameLiveData = null;

	Game(ZonedDateTime date, int gamePk, Team awayTeam, Team homeTeam, JSONObject rawGameData) {
		this.date = date;
		this.gamePk = gamePk;
		this.awayTeam = awayTeam;
		this.homeTeam = homeTeam;

		this.rawGameData = rawGameData;
	}

	public static Game parse(JSONObject rawScheduleGameJson) {
		try {
			ZonedDateTime date = DateUtils.parseNHLDate(rawScheduleGameJson.getString("gameDate"));
			int gamePk = rawScheduleGameJson.getInt("gamePk");
			Team awayTeam = Team.parse(rawScheduleGameJson.getJSONObject("teams").getJSONObject("away")
					.getJSONObject("team").getInt("id"));
			Team homeTeam = Team.parse(rawScheduleGameJson.getJSONObject("teams").getJSONObject("home")
					.getJSONObject("team").getInt("id"));
			Game game = new Game(date, gamePk, awayTeam, homeTeam, rawScheduleGameJson);

			return game;
		} catch (Exception e) {
			LOGGER.error("Could not parse game.", e);
			return null;
		}
	}

	public void updateGameData(JSONObject rawGameData) {
		LOGGER.trace("Updating Game Data. [" + gamePk + "]");
		this.rawGameData = rawGameData;
	}

	public void updateLiveData() {
		LOGGER.debug("Updating Game Live Data. [" + gamePk + "]");
		if (gameLiveData == null) {
			gameLiveData = GameLiveData.create(getGamePk());
		} else {
			gameLiveData.update();
		}
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
		if (getHomeScore() > getAwayScore()) {
			return homeTeam;
		}
		if (getAwayScore() > getHomeScore()) {
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

	public int getAwayScore() {
		return rawGameData.getJSONObject("teams").getJSONObject("away").getInt("score");
	}

	public int getHomeScore() {
		return rawGameData.getJSONObject("teams").getJSONObject("home").getInt("score");
	}

	public GameStatus getStatus() {
		return GameStatus.parse(rawGameData.getJSONObject("status"));
	}

	public List<GameEvent> getEvents() {
		return rawGameData.getJSONArray("scoringPlays").toList().stream()
				.map(HashMap.class::cast)
				.map(JSONObject::new)
				.map(GameEvent::parse)
				.collect(Collectors.toList());
	}


	@Override
	public String toString() {
		return "Game [getDate()=" + getDate() + ", getGamePk()=" + getGamePk() + ", getAwayTeam()=" + getAwayTeam()
				+ ", getHomeTeam()=" + getHomeTeam() + ", getWinningTeam()=" + getWinningTeam() + ", getTeams()="
				+ getTeams() + ", getAwayScore()=" + getAwayScore() + ", getHomeScore()=" + getHomeScore()
				+ ", getStatus()=" + getStatus() + ", getEvents()=" + getEvents() + "]";
	}

	public boolean equals(Game other) {
		return gamePk == other.gamePk;
	}
}
