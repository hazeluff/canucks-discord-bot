package com.hazeluff.discord.nhl;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.utils.DateUtils;
import com.hazeluff.discord.utils.HttpException;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.NHLGateway;
import com.hazeluff.nhl.PlayoffSeries;

/**
 * This class is used to start GameTrackers for games and to maintain the
 * channels in discord for those games.
 */
public class NHLPlayoffBracketFetcher extends Thread {

	private static final Logger LOGGER = LoggerFactory.getLogger(NHLPlayoffBracketFetcher.class);

	static final long UPDATE_RATE = 300000L;
	static final long RETRY_RATE = 100000L;

	private AtomicBoolean init = new AtomicBoolean(false);
	AtomicReference<ZonedDateTime> lastUpdate = new AtomicReference<>();
	

	Map<String, PlayoffSeries> playoffBracket;

	public NHLPlayoffBracketFetcher() {
		playoffBracket = new ConcurrentHashMap<>();
	}

	/**
	 * Starts the thread that sets up channels and polls for updates to
	 * NHLGameTrackers.
	 */
	@Override
	public void run() {
		LOGGER.info("Initializing...");
		try {
			LOGGER.info("Initializing Playoff Bracket...");
			updatePlayoffBracket();
			LOGGER.info("Finished initialization.");
		} catch (HttpException e) {
			LOGGER.error("Error occured when initializing games.", e);
			throw new RuntimeException(e);
		}

		init.set(true);
		LOGGER.info("Finished Initializing.");

		lastUpdate.set(DateUtils.now());
		while (!isStop() && !isInterrupted()) {
			LOGGER.info("Checking for update [lastUpdate={}]", getLastUpdate().toString());
			ZonedDateTime today = DateUtils.now();
			try {
				// Update every 20 minutes
				if (DateUtils.diffMinutes(getLastUpdate(), today) > 20) {
					LOGGER.info("Updating Playoff Bracket");
					updatePlayoffBracket();
					lastUpdate.set(today);
					LOGGER.info("Successfully updated games.");
				}
				Utils.sleep(UPDATE_RATE);
			} catch (Exception e) {
				LOGGER.error("Error occured when updating games.", e);
				Utils.sleep(RETRY_RATE);
			}
		}
	}

	void updatePlayoffBracket() throws HttpException {
		this.playoffBracket = NHLGateway.getPlayoffBracket(String.valueOf(Config.NHL_CURRENT_SEASON.getEndYear()));
	}

	public Map<String, PlayoffSeries> getPlayoffBracket() {
		return playoffBracket;
	}

	/*
	 * 
	 */
	boolean isStop() {
		return false;
	}

	public void setInit(boolean value) {
		init.set(value);
	}

	public boolean isInit() {
		return init.get();
	}

	public ZonedDateTime getLastUpdate() {
		return lastUpdate.get();
	}
}
