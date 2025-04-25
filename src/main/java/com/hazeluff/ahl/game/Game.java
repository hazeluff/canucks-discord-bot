package com.hazeluff.ahl.game;

import java.time.LocalDate;
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

	public LocalDate getDate() {
		return date;
	}

	public Team getHomeTeam() {
		return homeTeam;
	}

	public Team getAwayTeam() {
		return awayTeam;
	}

	public static Game parse(BsonDocument jsonScheduleGame) {
		int id = Integer.valueOf(
				jsonScheduleGame.getDocument("row")
						.getString("game_id")
						.getValue());
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
						.getDocument("home_team_city")
						.getString("teamLink")
						.getValue()));
		return new Game(id, date, homeTeam, awayTeam);
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
