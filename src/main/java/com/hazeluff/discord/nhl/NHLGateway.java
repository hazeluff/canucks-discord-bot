package com.hazeluff.discord.nhl;

import static com.hazeluff.discord.Config.CURRENT_SEASON;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.nhl.Seasons.Season;
import com.hazeluff.discord.utils.HttpException;
import com.hazeluff.discord.utils.HttpUtils;
import com.hazeluff.nhl.Team;

/**
 * Access to NHL API
 */
public class NHLGateway {
	private static final Logger LOGGER = LoggerFactory.getLogger(NHLGateway.class);

	// Paths/URLs
	static String getClubScheduleSeasonUrl(Team team, int startYear) {
		int endYear = startYear + 1;
		String season = startYear + "" + endYear;
		return Config.NHL_API_URL + "/club-schedule-season/" + team.getCode().toLowerCase() + "/" + season;
	}

	static String getPlayByPlayUrl(int gameId) {
		return Config.NHL_API_URL + "/gamecenter/" + gameId + "/play-by-play";
	}

	// Fetchers
	static String fetchRawGames(Team team, Season season) throws HttpException {
		URI uri = HttpUtils.buildUri(getClubScheduleSeasonUrl(team, season.getStartYear()));
		return HttpUtils.getAndRetry(uri, 2, 10000l, "Get Raw Games: team=" + team + ", season=" + season);
	}

	static String fetchRawPlayByPlay(int gameId) throws HttpException {
		URI uri = HttpUtils.buildUri(getPlayByPlayUrl(gameId));
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

	public static void main(String[] argv) throws HttpException {
		System.out.println(fetchRawPlayByPlay(2023010002));
	}
}
