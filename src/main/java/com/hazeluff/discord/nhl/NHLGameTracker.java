package com.hazeluff.discord.nhl;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bson.BsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.gdc.GameTracker;
import com.hazeluff.discord.bot.gdc.nhl.NHLGameDayChannelsManager;
import com.hazeluff.discord.utils.DateUtils;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.NHLGateway;
import com.hazeluff.nhl.game.Game;

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
public class NHLGameTracker extends Thread implements GameTracker {
	private static final Logger LOGGER = LoggerFactory.getLogger(NHLGameTracker.class);

	// Polling time for when game is not close to starting
	static final long IDLE_POLL_RATE_MS = 60000l;
	// Polling time for when game is started/almost-started
	public static final long ACTIVE_POLL_RATE_MS = 5000l;
	// Time before game to poll faster
	static final long CLOSE_TO_START_THRESHOLD_MS = 300000l;
	// Time after game is final to continue updates
	static final long POST_GAME_UPDATE_DURATION = 60000l;

	private static Map<Game, NHLGameTracker> gameTrackers = new ConcurrentHashMap<>();

	private final Game game;

	private AtomicBoolean started = new AtomicBoolean(false);
	private AtomicBoolean finished = new AtomicBoolean(false);

	NHLGameTracker(Game game) {
		this.game = game;
	}

	/**
	 * Gets an instance of a {@link NHLGameTracker} for the given game. The tracker
	 * thread is started on instantiation.
	 * 
	 * @param game
	 *            game to get {@link NHLGameTracker} for
	 * @return {@link NHLGameTracker} for the game
	 */
	public static NHLGameTracker get(Game game) {
		NHLGameTracker gameTracker = new NHLGameTracker(game);
		gameTracker.start();
		return gameTracker;
	}

	public void updateGame() {
		BsonDocument jsonPlayByPlay = NHLGateway.getPlayByPlay(this.game.getGameId());
		if (jsonPlayByPlay != null) {
			this.game.updatePlayByPlay(jsonPlayByPlay);
		}
		BsonDocument jsonBoxScore = NHLGateway.getBoxScore(this.game.getGameId());
		if (jsonBoxScore != null) {
			this.game.updateBoxScore(jsonBoxScore);
		}
		BsonDocument jsonRightRail = NHLGateway.getRightRail(this.game.getGameId());
		if (jsonRightRail != null) {
			this.game.updateRightRail(jsonRightRail);
		}
	}

	@Override
	public void start() {
		if (started.compareAndSet(false, true)) {
			LOGGER.info("Started thread for [" + game.getGameId() + "]");
			setName(NHLGameDayChannelsManager.buildChannelName(game));
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

			if (!game.getGameState().isFinished()) {
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
					started = game.getGameState().isStarted();
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
					if (game.getGameState().isFinished()) {
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

	/**
	 * Determines if this tracker is finished.
	 * 
	 * @return true, if this tracker is finished<br>
	 *         false, otherwise
	 */
	public boolean isFinished() {
		return finished.get();
	}

	public boolean isGameFinished() {
		return game.getGameState().isFinished();
	}

	public boolean isGameStarted() {
		return game.getGameState().isStarted();
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
