package com.hazeluff.nhl;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.bsonpatch.BsonPatch;
import com.ebay.bsonpatch.BsonPatchApplicationException;
import com.hazeluff.discord.Config;
import com.hazeluff.discord.utils.HttpException;
import com.hazeluff.discord.utils.HttpUtils;

/**
 * Class that has methods to patch the underlying JSON of a Game's Live data.
 */
public class GameLiveData {
	private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);

	private final int gamePk;
	private final BsonDocument rawJson;

	private GameLiveData(int gamePk, BsonDocument rawJson) {
		this.gamePk = gamePk;
		this.rawJson = rawJson;
	}

	public static GameLiveData create(int gamePk) {
		return new GameLiveData(gamePk, BsonDocument.parse(fetchDataJson(gamePk, null)));
	}

	public void update() {
		String strJsonDiffs = fetchDataJson(gamePk, getTimecode());
		BsonArray jsonDiffs = BsonArray.parse(strJsonDiffs);
		jsonDiffs.forEach(jsonDiff -> applyPatch(jsonDiff.asDocument().getArray("diff")));

	}

	private void applyPatch(BsonArray patches) {
		try {
			BsonPatch.applyInPlace(patches, getJson());
		} catch (BsonPatchApplicationException e) {
			LOGGER.warn("Could not process update: " + patches);
			LOGGER.warn(getJson().toString());
		}
	}

	private static String fetchDataJson(int gamePk, String timeCode) {
		String url = Config.NHL_API_URL + "/game/" + gamePk + "/feed/live";
		if (timeCode != null) {
			url += "/diffPatch";
		}

		URI uri = null;
		try {
			URIBuilder uriBuilder = new URIBuilder(url);
			uriBuilder.addParameter("site", "en_nhl");
			if (timeCode != null) {
				uriBuilder.addParameter("startTimecode", timeCode);
			}

			uri = uriBuilder.build();

			return HttpUtils.getAndRetry(uri, 
					3, // retries
					5000l, //
					"Update the game.");
		} catch (URISyntaxException e) {
			LOGGER.error("Exception building URI.", e);
		} catch (HttpException e) {
			LOGGER.error("Exception getting response.", e);
		}
		throw new RuntimeException("Could not fetch live data. gamePk=" + gamePk);
	}

	public BsonDocument getJson() {
		return rawJson;
	}

	public BsonDocument getGameData() {
		return getJson().getDocument("gameData");
	}

	public GameStatus getStatus() {
		return GameStatus.parse(getGameData().getDocument("status"));
	}

	public BsonDocument getLiveData() {
		return getJson().getDocument("liveData");
	}

	public BsonDocument getLinescore() {
		return getLiveData().getDocument("linescore");
	}
	
	public int getPeriod() {
		return getLinescore().getInt32("currentPeriod").getValue();
	}
	
	public String getPeriodOrdinal() {
		return getLinescore().getString("currentPeriodOrdinal").getValue();
	}
	
	public String getPeriodTimeRemaining() {
		return getLinescore().getString("currentPeriodTimeRemaining").getValue();
	}

	public boolean hasShootout() {
		return getLinescore().getBoolean("hasShootout").getValue();
	}

	public BsonDocument getIntermissionInfo() {
		return getLinescore().getDocument("intermissionInfo");
	}

	public boolean isIntermission() {
		return getIntermissionInfo().getBoolean("inIntermission").getValue();
	}

	public int getIntermissionTimeElapsed() {
		return getIntermissionInfo().getInt32("intermissionTimeElapsed").getValue();
	}

	public int getIntermissionTimeRemaining() {
		return getIntermissionInfo().getInt32("intermissionTimeRemaining").getValue();
	}

	public BsonDocument getPowerPlayInfo() {
		return getLinescore().getDocument("powerPlayInfo");
	}

	public boolean isPowerPlay() {
		return getPowerPlayInfo().getBoolean("inSituation").getValue();
	}

	public int getPowerPlayTimeElapsed() {
		return getPowerPlayInfo().getInt32("situationTimeElapsed").getValue();
	}

	public int getPowerPlayTimeRemaining() {
		return getPowerPlayInfo().getInt32("situationTimeRemaining").getValue();
	}
	
	public String getPowerPlayStrength() {
		return getLinescore().getString("powerPlayStrength").getValue();
	}

	public int getHomeScore() {
		return getLinescore().getDocument("teams").getDocument("home").getInt32("goals").getValue();
	}

	public int getHomeShots() {
		return getLinescore().getDocument("teams").getDocument("home").getInt32("shotsOnGoal").getValue();
	}

	public int getAwayScore() {
		return getLinescore().getDocument("teams").getDocument("away").getInt32("goals").getValue();
	}

	public int getAwayShots() {
		return getLinescore().getDocument("teams").getDocument("away").getInt32("shotsOnGoal").getValue();
	}

	public String getTimecode() {
		return getJson().getDocument("metaData").getString("timeStamp").getValue();
	}
}
