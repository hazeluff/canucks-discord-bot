package com.hazeluff.nhl.game.data;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.client.utils.URIBuilder;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonInvalidOperationException;
import org.bson.BsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.bsonpatch.BsonPatch;
import com.ebay.bsonpatch.BsonPatchApplicationException;
import com.hazeluff.discord.Config;
import com.hazeluff.discord.utils.HttpUtils;
import com.hazeluff.nhl.game.Game;
import com.hazeluff.nhl.game.LineScore;
import com.hazeluff.nhl.game.Status;

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
		liveData.resetLiveData();
		return liveData;
	}


	public void update() {
		try {
			if (getTimecode() != null) {
				try {
					patchLiveData();
				} catch (BsonPatchApplicationException | BsonInvalidOperationException e) {
					LOGGER.warn("Could not apply diffs. Fetching full data...");
					resetLiveData();
				}
			} else {
				resetLiveData();
			}
		} catch (Exception e) {
			LOGGER.warn("Failed to update. gamePk=" + gamePk, e);
		}
	}

	private void patchLiveData() {
		try {
			BsonDocument tempLiveData = getJson();
			BsonArray jsonDiffs = fetchDiffs(gamePk, getTimecode());
			for (BsonValue jsonDiff : jsonDiffs) {
				BsonPatch.applyInPlace(jsonDiff.asDocument().getArray("diff"), tempLiveData);
			}
			rawJson.set(tempLiveData);
		} catch (LiveDataException e) {
			LOGGER.warn("Failed to fetch data.", e);
		}
	}

	public void resetLiveData() {
		try {
			BsonDocument jsonLiveData = fetchLiveData(gamePk);
			rawJson.set(jsonLiveData);
		} catch (LiveDataException e) {
			LOGGER.warn("Failed to fetch data.", e);
		} catch (Exception e) {
			LOGGER.warn("Failed to reset. gamePk=" + gamePk, e);
		}
	}

	private static BsonDocument fetchLiveData(int gamePk) throws LiveDataException {
		return BsonDocument.parse(fetchDataJson(gamePk, null));
	}

	private static BsonArray fetchDiffs(int gamePk, String timeCode) throws LiveDataException {
		return BsonArray.parse(fetchDataJson(gamePk, timeCode));
	}

	private static String fetchDataJson(int gamePk, String timeCode) throws LiveDataException {
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
					"Fetch game data. gamePk=" + gamePk);
		} catch (Exception e) {
			throw new LiveDataException("Failed to fetch json.", e);
		}
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

	public BsonDocument getPlays() {
		return getLiveData().getDocument("plays");
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
