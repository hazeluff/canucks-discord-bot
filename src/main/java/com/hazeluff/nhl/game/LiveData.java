package com.hazeluff.nhl.game;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.client.utils.URIBuilder;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonInvalidOperationException;
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
	private AtomicReference<BsonDocument> rawJson = new AtomicReference<BsonDocument>(null);

	private LiveData(int gamePk) {
		this.gamePk = gamePk;
	}

	public static LiveData create(int gamePk) {
		LiveData liveData = new LiveData(gamePk);
		liveData.fetchLiveData();
		return liveData;
	}

	public void update() {
		if (getTimecode() != null) {
			BsonArray jsonDiffs = fetchPatch(gamePk, getTimecode());
			jsonDiffs.forEach(jsonDiff -> applyPatch(jsonDiff.asDocument().getArray("diff")));
		} else {
			fetchLiveData();
		}
	}

	private void applyPatch(BsonArray patches) {
		try {
			BsonPatch.applyInPlace(patches, getJson());
		} catch (BsonPatchApplicationException e) {
			LOGGER.warn("Could not apply patch: " + patches);
			fetchLiveData();
		}
	}

	public void fetchLiveData() {
		rawJson.set(fetchLiveData(gamePk));
	}

	private static BsonDocument fetchLiveData(int gamePk) {
		return BsonDocument.parse(fetchDataJson(gamePk, null));
	}

	private static BsonArray fetchPatch(int gamePk, String timeCode) {
		return BsonArray.parse(fetchDataJson(gamePk, timeCode));
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
		return rawJson.get();
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
		try {
			return getJson().getDocument("metaData").getString("timeStamp").getValue();
		} catch (BsonInvalidOperationException e) {
			return null;
		}

	}
}
