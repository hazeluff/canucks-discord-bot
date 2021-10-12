package com.hazeluff.nhl.game;

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
public class LiveData {
	private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);

	private final int gamePk;
	private final BsonDocument rawJson;

	private LiveData(int gamePk, BsonDocument rawJson) {
		this.gamePk = gamePk;
		this.rawJson = rawJson;
	}

	public static LiveData create(int gamePk) {
		return new LiveData(gamePk, BsonDocument.parse(fetchDataJson(gamePk, null)));
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

	protected BsonDocument getJson() {
		return rawJson;
	}

	protected BsonDocument getGameData() {
		return getJson().getDocument("gameData");
	}

	public Status getStatus() {
		return Status.parse(getGameData().getDocument("status"));
	}

	protected BsonDocument getLiveData() {
		return getJson().getDocument("liveData");
	}

	public LineScore getLinescore() {
		return LineScore.parse(getLiveData().getDocument("linescore"));
	}

	public String getTimecode() {
		return getJson().getDocument("metaData").getString("timeStamp").getValue();
	}
}
