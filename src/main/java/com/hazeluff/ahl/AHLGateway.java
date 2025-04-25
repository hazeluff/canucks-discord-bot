package com.hazeluff.ahl;

import java.net.URI;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.utils.HttpException;
import com.hazeluff.discord.utils.HttpUtils;
import com.hazeluff.discord.utils.Utils;

/**
 * Access to NHL API
 */
public class AHLGateway {
	private static final Logger LOGGER = LoggerFactory.getLogger(AHLGateway.class);

	// Paths/URLs
	static String getConfigUrl() {
		return Config.AHL_API_CONFIG_URL;
	}

	private static String appendApiKey(String path) {
		return path + "&key=" + Config.AHL_API_KEY
				+ "&client_code=" + Config.AHL_API_CLIENT_CODE;
	}

	static String buildScheduleUrl(int team, int season, int month) {
		String url = Config.AHL_API_URL + "/feed/index.php?feed=statviewfeed&view=schedule"
				+ "&team=" + team
				+ "&season=" + season
				+ "&month=" + month;
		return appendApiKey(url);
	}

	static String buildGameSummaryUrl(int gameId) {
		String url = Config.AHL_API_URL + "/feed/index.php?feed=statviewfeed&view=gameSummary"
				+ "&game_id=" + gameId;
		return appendApiKey(url);
	}

	// Fetchers
	static String fetchConfig() throws HttpException {
		URI uri = HttpUtils.buildUri(getConfigUrl());
		return HttpUtils.get(uri);
	}

	static String fetchSchedule(int team, int season, int month) throws HttpException {
		URI uri = HttpUtils.buildUri(buildScheduleUrl(team, season, month));
		return HttpUtils.get(uri);
	}

	static String fetchGameSummary(int gameId) throws HttpException {
		URI uri = HttpUtils.buildUri(buildGameSummaryUrl(gameId));
		return HttpUtils.get(uri);
	}

	// Interface
	public static String[] getConfig() {
		try {
			String strConfig;
			strConfig = fetchConfig();
			String appKey = Utils.regexCapture(strConfig, "var\\s*appKey\\s*=\\s*'(\\w*)'");
			String clientCode = Utils.regexCapture(strConfig, "var\\s*clientCode\\s*=\\s*\"(\\w*)\"");
			return new String[] { appKey, clientCode };
		} catch (HttpException e) {
			throw new RuntimeException(e);
		}
	}

	public static BsonArray getSchedule(int team, int season, int month) {
		try {
			String strJsonBracket = fetchSchedule(team, season, month);
			strJsonBracket = stripParentheses(strJsonBracket);
			BsonArray array = BsonArray.parse(strJsonBracket);
			return array.get(0).asDocument().getArray("sections").get(0).asDocument().getArray("data");
		} catch (HttpException e) {
			LOGGER.error("Exception occured fetching schedule.", e);
			return null;
		}
	}

	public static BsonDocument getGameSummary(int gameId) {
		try {
			String strJsonBracket = fetchGameSummary(gameId);
			strJsonBracket = stripParentheses(strJsonBracket);
			return BsonDocument.parse(strJsonBracket);
		} catch (HttpException e) {
			LOGGER.error("Exception occured fetching game summary.", e);
			return null;
		}
	}

	private static String stripParentheses(String str) {
		return str.substring(1, str.length() - 1);
	}
}
