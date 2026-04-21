package com.hazeluff.discord.bot.gdc;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMeta;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.bot.gdc.nhl.NHLFormatter;
import com.hazeluff.discord.utils.InterruptableThread;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.game.NHLGame;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;

public abstract class GameDayThread extends InterruptableThread {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameDayThread.class);

	protected Logger LOGGER() {
		return LOGGER;
	}

	// Polling time for when game is not close to starting
	static final long IDLE_POLL_RATE_MS = 300000l;
	// Polling time for when game is started/almost-started
	protected static final long ACTIVE_POLL_RATE_MS = 10000l;
	// Time before game to poll faster
	static final long CLOSE_TO_START_THRESHOLD_MS = 300000l;

	protected final NHLBot nhlBot;
	
	protected final GameTracker gameTracker;
	protected final Guild guild;
	protected final MessageChannel channel;
	protected final GDCMeta meta;

	protected Message introMessage;
	protected Message summaryMessage;
	protected EmbedCreateSpec summaryMessageEmbed; // Used to determine if message needs updating.

	protected AtomicBoolean started = new AtomicBoolean(false);

	protected GameDayThread(NHLBot nhlBot, GameTracker gameTracker, Guild guild, MessageChannel channel, GDCMeta meta) {
		this.nhlBot = nhlBot;
		this.gameTracker = gameTracker;
		this.guild = guild;
		this.channel = channel;
		this.meta = meta;
	}

	/*
	 * Thread
	 */

	@Override
	public void start() {
		if (started.compareAndSet(false, true)) {
			superStart();
		} else {
			LOGGER().warn("Thread already started.");
		}
	}

	void superStart() {
		super.start();
	}

	@Override
	public void run() {
		try {
			_run();
		} catch (Exception e) {
			LOGGER().error("Error occurred while running thread.", e);
		} finally {
			LOGGER().info("Thread completed");
		}
	}

	protected void _run() {
		setThreadName();
		LOGGER().info("Started thread.");

		if (gameTracker.isGameFinished()) {
			LOGGER().info("Game is already finished");
			return;
		}

		// (Pre-Game) Init + Start of Game
		try {
			// Wait for Game to be initialized
			while (!isGameInit() && !isInterrupted()) {
				if (gameTracker.isGameFinished()) {
					LOGGER().error("GameTracker finished while waiting for Game initialization.");
					return;
				}
				sleepFor(5000);
			}

			initChannel(); // ## Overridable ##

			// Wait until close to start of game
			waitAndSendReminders();

			// Game is close to starting. Poll at higher rate than previously
			LOGGER().info("Game is about to start. Polling more actively.");
			boolean alreadyStarted = waitForStart();

			// Game has started
			if (!alreadyStarted) {
				LOGGER().info("Game is about to start!");
				_updateStart();
			} else {
				LOGGER().info("Game has already started.");
			}
		} catch (Exception e) {
			LOGGER().error("Exception occured during start of Thread.", e);
		}

		// (In-Progress) Game Updates
		while (!gameTracker.isFinished() && !isInterrupted()) {
			try {
				updateActive(); // ## Overridable ##
			} catch (Exception e) {
				LOGGER().error("Exception occured while running.", e);
			}
			sleepFor(ACTIVE_POLL_RATE_MS);
		}

		// (Post-Game)
		_updateEnd(); // ## Overridable ##
	}

	/*
	 * Run method overrides
	 */	
	protected abstract void setThreadName();

	protected abstract long timeUntilGame();

	protected void updateStart() {
		sendStartOfGameMessage();
	}

	/**
	 * Override if game initialization needs to be waited on.
	 * 
	 * @return
	 */
	protected boolean isGameInit() {
		return true;
	}
	protected abstract void initChannel();
	protected abstract void updateActive();
	protected abstract void updateEnd();

	/**
	 * Does actions for the start of the game.
	 * 
	 * @param game
	 *            game of which it's channels will have the messages sent to
	 */
	protected void _updateStart() {
		LOGGER().info("Sending start message.");
		if (!isInterrupted()) {
			updateStart();
		}
	}

	private void _updateEnd() {
		if (!isInterrupted()) {
			updateEnd();
		}
	}

	/**
	 * Sends reminders of time till the game starts.
	 * 
	 * @throws InterruptedException
	 */
	protected void waitAndSendReminders() {
		boolean firstPass = true;
		boolean closeToStart;
		long timeTillGameMs = Long.MAX_VALUE;
		Map<Long, String> reminders = getReminders();
		LOGGER().info("Waiting for game to start.");
		do {
			timeTillGameMs = timeUntilGame();
			closeToStart = timeTillGameMs < CLOSE_TO_START_THRESHOLD_MS;
			if (!closeToStart) {
				// Check to see if message should be sent.
				long lowestThreshold = Long.MAX_VALUE;
				String message = null;
				Iterator<Entry<Long, String>> it = reminders.entrySet().iterator();
				while (it.hasNext() && !isInterrupted()) {
					Entry<Long, String> entry = it.next();
					long threshold = entry.getKey();
					if (threshold > timeTillGameMs) {
						if (lowestThreshold > threshold) {
							lowestThreshold = threshold;
							message = buildReminderMessage(entry.getValue());
						}
						it.remove();
					}
				}
				if (message != null && !firstPass && !isInterrupted()) {
					sendMessage(message);
				}
				lowestThreshold = Long.MAX_VALUE;
				message = null;
				firstPass = false;
				LOGGER().trace("Idling until near game start. Sleeping for [" + IDLE_POLL_RATE_MS + "]");

				sleepFor(IDLE_POLL_RATE_MS);
			}
		} while (!closeToStart && !isInterrupted());
	}

	protected abstract Map<Long, String> getReminders();

	protected String buildReminderMessage(String basicMessage) {
		return basicMessage;
	}

	/**
	 * Polls at higher polling rate before game starts. Returns whether or not the
	 * game has already started
	 * 
	 * @return true, if game is already started<br>
	 *         false, otherwise
	 * @throws InterruptedException
	 */
	protected boolean waitForStart() {
		boolean alreadyStarted = gameTracker.isGameStarted();
		boolean started = false;
		do {
			started = gameTracker.isGameStarted();
			if (!started && !isInterrupted()) {
				LOGGER().trace("Game almost started. Sleeping for [" + ACTIVE_POLL_RATE_MS + "]");
				sleepFor(ACTIVE_POLL_RATE_MS);
			}
		} while (!started && !isInterrupted());
		return alreadyStarted;
	}

	protected void sendStartOfGameMessage() {
		sendMessage(buildStartOfGameMessage());
	}

	protected String buildStartOfGameMessage() {
		List<String> messageList = Arrays.asList(
			"Game is about to start!",
			"Lets go team!",
			"Be Kind, Be Calm, Be Safe",
				"Be woke, be cool, a calm spirit is smarter.", "Get ready, go to the washroom, get your snacks, "
						+ "get your drinks, get your ????, " + "get comfy, and watch us play.",
			"I just hope everybody has fun",
			"Good Luck; Have Fun"
		);
		return Utils.getRandom(messageList);
	}

	/*
	 * Discord Convenience Methods
	 */
	protected void sendMessage(String message) {
		if (channel != null) {
			DiscordManager.sendMessage(channel, message);
		}
	}

	protected void sendMessage(MessageCreateSpec spec) {
		if (channel != null) {
			DiscordManager.sendMessage(channel, spec);
		}
	}

	public void unpinSummaryMessage() {
		if (summaryMessage != null) {
			DiscordManager.unpinMessage(summaryMessage);
		}
	}

	boolean isBotSelf(User user) {
		return user.getId().equals(nhlBot.getDiscordManager().getId());
	}

	/*
	 * Getters
	 */

	public Guild getGuild() {
		return guild;
	}

	/**
	 * Gets the message that NHLBot will respond with when queried about this game
	 * 
	 * @param game
	 *            the game to get the message for
	 * @param timeZone
	 *            the time zone to localize to
	 * 
	 * @return message in the format: "The next game is:\n<br>
	 *         **Home Team** vs **Away Team** at HH:mm aaa on EEEE dd MMM yyyy"
	 */
	public static String buildDetailsMessage(NHLGame game) {
		String time = game.isStartTimeTBD()
				? "`TBD`"
				: String.format("<t:%s>", game.getStartTime().toEpochSecond());
		String message = String.format(
				"**%s** vs **%s** at %s", 
				game.getHomeTeam().getFullName(), game.getAwayTeam().getFullName(), 
				time
			);
		return message;
	}
	
	/**
	 * Gets the message that NHLBot will respond with when queried about this game
	 * 
	 * @param game
	 *            the game to get the message for
	 * @param timeZone
	 *            the time zone to localize to
	 * 
	 * @return message in the format: "The next game is:\n<br>
	 *         **Home Team** vs **Away Team** at HH:mm aaa on EEEE dd MMM yyyy"
	 */
	public static String buildThreadTitle(NHLGame game) {
		String message = String.format(
			"%s vs %s [%s]",
			game.getHomeTeam().getCode(), 
			game.getAwayTeam().getCode(),
			NHLFormatter.getThreadDate(game)
		);
		return message;
	}
}
