package com.hazeluff.ahl.game;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.ahl.game.event.EventType;
import com.hazeluff.ahl.game.event.GoalEvent;
import com.hazeluff.ahl.game.event.PenaltyEvent;
import com.hazeluff.ahl.game.event.ShootoutEvent;
import com.hazeluff.discord.Config;
import com.hazeluff.discord.ahl.AHLTeams.Team;


public class Game {
	private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);

	private final int id;
	private final LocalDate date;
	final Team homeTeam;
	final Team awayTeam;

	private GameSummaryData gsData = null;
	private PlayByPlayData pbpData = null;

	private Game(int id, LocalDate date, Team homeTeam, Team awayTeam) {
		this.id = id;
		this.date = date;
		this.homeTeam = homeTeam;
		this.awayTeam = awayTeam;
	}

	public int getId() {
		return id;
	}

	public String getNiceName() {
		String niceName = String.format(
			"%.3s-vs-%.3s-%s",
			getHomeTeam() == null ? "null" : getHomeTeam().getTeamCode(),
			getAwayTeam() == null ? "null" : getAwayTeam().getTeamCode(),
			getNiceShortDate()
		);
		return niceName.toLowerCase();
	}

	public LocalDate getDate() {
		return date;
	}

	public String getNiceDate() {
		return date.format(DateTimeFormatter.ofPattern("EEEE, d/MMM/yyyy"));
	}

	public String getNiceShortDate() {
		return date.format(DateTimeFormatter.ofPattern("yy-MM-dd"));
	}

	public Team getHomeTeam() {
		return homeTeam;
	}

	public Team getAwayTeam() {
		return awayTeam;
	}

	public Team getOppossingTeam(Team team) {
		if (!containsTeam(team)) {
			return null;
		}
		return getTeams().stream().filter(gameTeam -> !gameTeam.equals(team)).findAny().orElse(null);
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
		boolean isHome = getHomeTeam() != null && getHomeTeam().equals(team);
		boolean isAway = getAwayTeam() != null && getAwayTeam().equals(team);
		return isHome || isAway;
	}

	public static Game parse(BsonDocument jsonScheduleGame) {
		int id = parseId(jsonScheduleGame);
		LocalDate date = parseDate(
				jsonScheduleGame.getDocument("row")
						.getString("date_with_day")
						.getValue());
		Team homeTeam = Team.parse(Integer.valueOf(
				jsonScheduleGame.getDocument("prop")
						.getDocument("home_team_city")
						.getString("teamLink")
						.getValue()));
		Team awayTeam = Team.parse(Integer.valueOf(
				jsonScheduleGame.getDocument("prop")
						.getDocument("visiting_team_city")
						.getString("teamLink")
						.getValue()));
		return new Game(id, date, homeTeam, awayTeam);
	}

	public static int parseId(BsonDocument jsonSceduleGame) {
		return Integer.valueOf(jsonSceduleGame
				.getDocument("row")
				.getString("game_id")
				.getValue()
		);
	}

	private static LocalDate parseDate(String strDate) {
		Pattern pattern = Pattern.compile("\\w*, (\\w*) (\\d*)");
		Matcher matcher = pattern.matcher(strDate);
		if (matcher.find()) {
			int month = getMonth(matcher.group(1));
			int year = getYear(month);
			int day = Integer.valueOf(matcher.group(2));

			return LocalDate.of(year, month, day);
		} else {
			return null;
		}
	}

	private static int getMonth(String month) {
		switch (month.toLowerCase()) {
		case "jan":
			return 1;
		case "feb":
			return 2;
		case "mar":
			return 3;
		case "apr":
			return 4;
		case "may":
			return 5;
		case "jun":
			return 6;
		case "jul":
			return 7;
		case "aug":
			return 8;
		case "sep":
			return 9;
		case "oct":
			return 10;
		case "nov":
			return 11;
		case "dec":
			return 12;
		default:
			return -1;
		}
	}

	private static int getYear(int month) {
		if (month >= 9) {
			return Config.AHL_CURRENT_SEASON.getStartYear();
		}
		return Config.AHL_CURRENT_SEASON.getEndYear();
	}

	// Game Summary
	void initGameSummary(BsonDocument jsonSummary) {
		GameSummaryData newSummaryData = GameSummaryData.parse(jsonSummary);
		if (newSummaryData != null) {
			this.gsData = newSummaryData;
		} else {
			LOGGER.error("Could not parse json: jsonSummary=" + jsonSummary);
		}
	}

	public void updateGameSummary(BsonDocument jsonSummary) {
		LOGGER.debug("Updating Game Summary Data. [" + getId() + "]");
		if (this.gsData != null) {
			this.gsData.update(jsonSummary);
		} else {
			initGameSummary(jsonSummary);
		}
	}

	public int getHomeScore() {
		if (gsData == null) {
			return 0;
		}
		return gsData.getHomeGoals();
	}

	public int getAwayScore() {
		if (gsData == null) {
			return 0;
		}
		return gsData.getAwayGoals();
	}

	public ZonedDateTime getStartTime() {
		if (gsData == null) {
			return null;
		}
		return gsData.getStartTime();
	}

	public boolean isStarted() {
		if (gsData == null) {
			return false;
		}
		return gsData.isStarted();
	}

	public boolean isFinished() {
		if (gsData == null) {
			return false;
		}
		return gsData.isFinal();
	}

	public boolean isLive() {
		if (gsData == null) {
			return false;
		}
		return gsData.isStarted() && !gsData.isFinal();
	}

	public String getStatus() {
		if (isLive()) {
			return "LIVE";
		} else if (isFinished()) {
			return "FINAL";
		} else if (!isStarted()) {
			return "NOTSTARTED";
		}

		return "UNKNOWN";
	}

	// Play By Play
	void initPlayByPlay(BsonArray jsonPbp) {
		PlayByPlayData newPbpData = PlayByPlayData.parse(jsonPbp);
		if (newPbpData != null) {
			this.pbpData = newPbpData;
		} else {
			LOGGER.error("Could not parse json: jsonPbp=" + jsonPbp);
		}
	}

	public void updatePlayByPlay(BsonArray jsonPbp) {
		LOGGER.debug("Updating Game Play-by-play Data. [" + getId() + "]");
		if (this.pbpData != null) {
			this.pbpData.update(jsonPbp);
		} else {
			initPlayByPlay(jsonPbp);
		}
	}

	public List<GoalEvent> getGoalEvents() {
		return pbpData.getPlays().stream()
				.filter(event -> event.getType() == EventType.GOAL)
				.map(GoalEvent.class::cast)
				.collect(Collectors.toList());
	}

	public List<PenaltyEvent> getPenaltyEvents() {
		return pbpData.getPlays().stream()
				.filter(event -> event.getType() == EventType.PENALTY)
				.map(PenaltyEvent.class::cast)
				.collect(Collectors.toList());
	}

	public List<ShootoutEvent> getShootoutEvents() {
		return pbpData.getPlays().stream()
				.filter(event -> event.getType() == EventType.SHOOTOUT)
				.map(ShootoutEvent.class::cast)
				.collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return "Game [id=" + id + ", date=" + date + ", homeTeam=" + homeTeam + ", awayTeam=" + awayTeam + "]";
	}
}
