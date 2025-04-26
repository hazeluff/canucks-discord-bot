package com.hazeluff.discord.ahl;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.ahl.AHLGateway;
import com.hazeluff.ahl.game.Game;
import com.hazeluff.discord.bot.gdc.GameTracker;
import com.hazeluff.discord.utils.DateUtils;
import com.hazeluff.discord.utils.Utils;

/**
 * <p>
 * Creates Channels in Guilds that are subscribed to the teams in this game.
 * </p>
 * 
 * <p>
 * Creates a thread that updates a {@link NHLGame}
 * </p>
 * 
 * <p>
 * Events are sent as messages to the channels created.
 * </p>
 */
public class AHLGameTracker extends Thread implements GameTracker {
	private static final Logger LOGGER = LoggerFactory.getLogger(AHLGameTracker.class);

	// Polling time for when game is not close to starting
	static final long IDLE_POLL_RATE_MS = 60000l;
	// Polling time for when game is started/almost-started
	public static final long ACTIVE_POLL_RATE_MS = 5000l;
	// Time before game to poll faster
	static final long CLOSE_TO_START_THRESHOLD_MS = 300000l;
	// Time after game is final to continue updates
	static final long POST_GAME_UPDATE_DURATION = 300000l;

	private static Map<Game, AHLGameTracker> gameTrackers = new ConcurrentHashMap<>();

	private final Game game;

	private AtomicBoolean started = new AtomicBoolean(false);
	private AtomicBoolean finished = new AtomicBoolean(false);

	AHLGameTracker(Game game) {
		this.game = game;
	}

	/**
	 * Gets an instance of a {@link AHLGameTracker} for the given game. The tracker
	 * thread is started on instantiation.
	 * 
	 * @param game
	 *            game to get {@link AHLGameTracker} for
	 * @return {@link AHLGameTracker} for the game
	 */
	public static AHLGameTracker get(Game game) {
		AHLGameTracker gameTracker = new AHLGameTracker(game);
		gameTracker.start();
		return gameTracker;
	}

	public void updateGame() {
		BsonArray jsonPlayByPlay = AHLGateway.getGamePlayByPlay(this.game.getId());
		if (jsonPlayByPlay != null) {
			this.game.updatePlayByPlay(jsonPlayByPlay);
		}

		BsonDocument jsonSummary = AHLGateway.getGameSummary(this.game.getId());
		if (jsonSummary != null) {
			this.game.updateGameSummary(jsonSummary);
		}
	}

	@Override
	public void start() {
		if (started.compareAndSet(false, true)) {
			LOGGER.info("Started thread for [" + game.getId() + "]");
			setThreadName();
			superStart();
		} else {
			LOGGER.warn("Thread already started.");
		}
	}

	void superStart() {
		super.start();
	}

	@Override
	public void run() {
		try {
			updateGame();

			if (!game.isFinished()) {
				// Wait until close to start of game
				LOGGER.info("Idling until near game start.");
				boolean closeToStart;
				long timeTillGameMs = Long.MAX_VALUE;
				ZonedDateTime gameStart = game.getStartTime();
				do {
					timeTillGameMs = DateUtils.diffMs(ZonedDateTime.now(), gameStart);
					closeToStart = timeTillGameMs < CLOSE_TO_START_THRESHOLD_MS;
					updateGame();
					if (!closeToStart) {
						LOGGER.trace("Idling until near game start. Sleeping for [" + IDLE_POLL_RATE_MS + "]");
						Utils.sleep(IDLE_POLL_RATE_MS);
					}
				} while (!closeToStart);
				LOGGER.info("Game is about to start. Polling more actively.");
				// Game is close to starting. Poll at higher rate than previously
				
				// Wait for start of game
				boolean started = false;
				do {
					updateGame();
					started = game.isStarted();
					if (!started) {
						Utils.sleep(ACTIVE_POLL_RATE_MS);
					}
				} while (!started);
				// - wait for start of game
				
				// Game has started
				LOGGER.info("Game has started.");
				
				// Main update loop
				ZonedDateTime lastFinal = null;
				boolean stopUpdates = false;
				do {
					updateGame();

					// Loop terminates when the GameStatus is Final and 10 minutes has elapsed
					if (game.isFinished()) {
						if (lastFinal == null) {
							LOGGER.debug("Game finished. Continuing polling...");
							lastFinal = ZonedDateTime.now();
						}
						long timeAfterFinal = DateUtils.diffMs(lastFinal, ZonedDateTime.now());
						LOGGER.debug("Game finished. timeAfterFinal={}(ms)", timeAfterFinal);

						stopUpdates = timeAfterFinal > POST_GAME_UPDATE_DURATION;
					} else {
						lastFinal = null;
						LOGGER.debug("Game not finished.");
					}

					Utils.sleep(ACTIVE_POLL_RATE_MS);
				} while (!stopUpdates);
				// - main update loop
				
				LOGGER.info("Game thread finished");
			} else {
				LOGGER.info("Game is already finished");
			}
		} finally {
			gameTrackers.remove(game);
			finished.set(true);
			LOGGER.info("Thread Completed");
		}
	}

	private void setThreadName() {
		setName(buildThreadName(game));
	}

	/**
	 * Gets the name that a channel in Discord related to this game would have.
	 * 
	 * @param game
	 *            game to get channel name for
	 * @return channel name in format: "AAA-vs-BBB-yy-MM-DD". <br>
	 *         AAA is the 3 letter code of home team<br>
	 *         BBB is the 3 letter code of away team<br>
	 *         yy-MM-DD is a date format
	 */
	public static String buildThreadName(Game game) {
		String channelName = String.format("%s-vs-%s-%s", game.getHomeTeam().getTeamCode(),
				game.getAwayTeam().getTeamCode(), buildChannelDate(game));
		return channelName.toLowerCase();

	}

	/**
	 * Gets the date in the format "yy-MM-dd"
	 * 
	 * @param game
	 *            game to get the date from
	 * @param zone
	 *            time zone to convert the time to
	 * @return the date in the format "yy-MM-dd"
	 */
	public static String buildChannelDate(Game game) {
		return game.getDate().format(DateTimeFormatter.ofPattern("yy-MM-dd"));
	}

	/**
	 * Determines if this tracker is finished.
	 * 
	 * @return true, if this tracker is finished<br>
	 *         false, otherwise
	 */
	public boolean isFinished() {
		return finished.get();
	}

	@Override
	public boolean isGameFinished() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isGameStarted() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Gets the game that is tracked.
	 * 
	 * @return NHLGame being tracked
	 */
	public Game getGame() {
		return game;
	}
}
