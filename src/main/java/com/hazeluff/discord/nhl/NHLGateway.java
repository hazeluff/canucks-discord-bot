package com.hazeluff.discord.nhl;

import static com.hazeluff.discord.Config.CURRENT_SEASON;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.nhl.Seasons.Season;
import com.hazeluff.discord.nhl.stats.GoalieStats;
import com.hazeluff.discord.nhl.stats.SkaterStats;
import com.hazeluff.discord.nhl.stats.TeamPlayerStats;
import com.hazeluff.discord.nhl.stats.TeamStandings;
import com.hazeluff.discord.utils.HttpException;
import com.hazeluff.discord.utils.HttpUtils;
import com.hazeluff.nhl.Team;

/**
 * Access to NHL API
 */
public class NHLGateway {
	private static final Logger LOGGER = LoggerFactory.getLogger(NHLGateway.class);

	// Paths/URLs
	static String getClubScheduleSeasonUrl(String teamCode, int startYear) {
		int endYear = startYear + 1;
		String season = startYear + "" + endYear;
		return Config.NHL_API_URL + "/club-schedule-season/" + teamCode.toLowerCase() + "/" + season;
	}

	static String getPlayByPlayUrl(int gameId) {
		return Config.NHL_API_URL + "/gamecenter/" + gameId + "/play-by-play";
	}
	
	static String getTeamPlayerStatsUrl(String teamCode, int startYear) {
		int endYear = startYear + 1;
		String season = startYear + "" + endYear;
		return Config.NHL_API_URL + "/club-stats/" + teamCode.toLowerCase() + "/" + season + "/2";
	}

	static String getStandingsSeasonsUrl() {
		return Config.NHL_API_URL + "/standings-season/";
	}

	static String getStandingsUrl(String seasonEnd) {
		return Config.NHL_API_URL + "/standings/" + seasonEnd;
	}

	// Fetchers
	static String fetchRawGames(Team team, Season season) throws HttpException {
		URI uri = HttpUtils.buildUri(getClubScheduleSeasonUrl(team.getCode(), season.getStartYear()));
		return HttpUtils.getAndRetry(uri, 2, 10000l, "Get Raw Games: team=" + team + ", season=" + season);
	}

	static String fetchRawPlayByPlay(int gameId) throws HttpException {
		URI uri = HttpUtils.buildUri(getPlayByPlayUrl(gameId));
		return HttpUtils.get(uri);
	}

	static String fetchTeamPlayerStats(Team team, Season season) throws HttpException {
		URI uri = HttpUtils.buildUri(getTeamPlayerStatsUrl(team.getCode(), season.getStartYear()));
		return HttpUtils.get(uri);
	}

	static String fetchStandingsSeasons() throws HttpException {
		URI uri = HttpUtils.buildUri(getStandingsSeasonsUrl());
		return HttpUtils.get(uri);
	}

	static String fetchStandings(String seasonEnd) throws HttpException {
		URI uri = HttpUtils.buildUri(getStandingsUrl(seasonEnd));
		return HttpUtils.get(uri);
	}

	// Interface
	public static Map<Integer, BsonDocument> getAllTeamRawGames() {
		Map<Integer, BsonDocument> allGames = new HashMap<>();
		for (Team team : Team.values()) {
			Map<Integer, BsonDocument> teamGames = NHLGateway.getTeamRawGames(team, CURRENT_SEASON);
			allGames.putAll(teamGames);
		}
		return allGames;
	}

	public static Map<Integer, BsonDocument> getTeamRawGames(Team team, Season season) {
		LOGGER.info("Retrieving games of [" + team + "]");
		Map<Integer, BsonDocument> games = new HashMap<>();
		try {
			String strJSONSchedule = fetchRawGames(team, season);
			BsonDocument jsonSchedule = BsonDocument.parse(strJSONSchedule);
			BsonArray jsonGames = jsonSchedule.getArray("games");
			jsonGames.forEach(jsonGame -> {
				int gameId = jsonGame.asDocument().getInt32("id", new BsonInt32(-1)).getValue();
				if (gameId > 0) {
					LOGGER.debug("Adding additional game [" + gameId + "]");
					games.put(gameId, jsonGame.asDocument());
				} else {
					LOGGER.warn("Could not parse 'id': " + jsonGame.toString());
				}
			});
		} catch (HttpException e) {
			LOGGER.error("Exception occured fetching game schedule.", e);
		}
		return games;
	}

	public static BsonDocument getPlayByPlay(int gameId) {
		try {
			String strBoxscore = fetchRawPlayByPlay(gameId);
			return BsonDocument.parse(strBoxscore);
		} catch (HttpException e) {
			LOGGER.error("Failed to get boxscore for game: " + gameId);
			return null;
		}
	}

	public static TeamPlayerStats getTeamPlayerStats(Team team, Season season) {
		try {
			String strBoxscore = fetchTeamPlayerStats(team, season);
			BsonDocument jsonPlayerStats = BsonDocument.parse(strBoxscore);
			List<GoalieStats> goalies = jsonPlayerStats.getArray("goalies").stream()
					.map(BsonValue::asDocument)
					.map(GoalieStats::parse)
					.collect(Collectors.toList());
			List<SkaterStats> skaters = jsonPlayerStats.getArray("skaters").stream()
					.map(BsonValue::asDocument)
					.map(SkaterStats::parse)
					.collect(Collectors.toList());
			return new TeamPlayerStats(goalies, skaters);
		} catch (HttpException e) {
			LOGGER.error("Failed to get team stats: team=" + team + ", season=" + season);
			return null;
		}
	}

	public static Map<Integer, String> getStandingsSeasonsEnds() {
		try {
			String strJsonSeasons = fetchStandingsSeasons();
			BsonArray jsonSeasons = BsonDocument.parse(strJsonSeasons).getArray("seasons");
			return jsonSeasons.stream()
					.map(BsonValue::asDocument)
					.collect(Collectors.toMap(
							jsonSeason -> mapStandingsEndToStartYear(jsonSeason.getString("standingsStart").getValue()),
							jsonSeason -> jsonSeason.getString("standingsEnd").getValue()));
		} catch (HttpException e) {
			LOGGER.error("Exception occured fetching game schedule.", e);
			return null;
		}
	}

	public static List<TeamStandings> getStandings(String endDate) {
		try {
			String strJsonStandings = fetchStandings(endDate);
			BsonArray jsonStandings = BsonDocument.parse(strJsonStandings).getArray("standings");
			return jsonStandings.stream()
					.map(BsonValue::asDocument)
					.map(TeamStandings::parse)
					.collect(Collectors.toList());
		} catch (HttpException e) {
			LOGGER.error("Exception occured fetching game schedule.", e);
			return null;
		}
	}

	/**
	 * Maps the standingsEnd date to the correct start year.
	 * 
	 * @param standingsEnd
	 * @return
	 */
	static int mapStandingsEndToStartYear(String standingsEnd) {
		switch (standingsEnd) {
		case "1995-01-20": // Lockout
			return 1994;
		case "2013-01-19": // Lockout
			return 2012;
		case "2021-01-13": // Covid
			return 2020;
		default:
			// Strip from end date
			return Integer.parseInt(standingsEnd.split("-")[0]);
		}
	}
}
