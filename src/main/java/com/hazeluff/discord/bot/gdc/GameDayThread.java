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
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.bot.listener.IEventProcessor;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.game.Game;

import discord4j.core.event.domain.Event;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;

public abstract class GameDayThread extends Thread implements IEventProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameDayThread.class);

	protected Logger LOGGER() {
		return LOGGER;
	}

	// Number of retries to do when NHL API returns no events.
	static final int NHL_EVENTS_RETRIES = 5;

	// Time to wait before tryign to fetch the Discord channel
	static final long CHANNEL_FETCH_RETRY_RATE_MS = 60000l;
	// Polling time for when game is not close to starting
	static final long IDLE_POLL_RATE_MS = 60000l;
	// Polling time for when game is started/almost-started
	protected static final long ACTIVE_POLL_RATE_MS = 10000l;
	// Time before game to poll faster
	static final long CLOSE_TO_START_THRESHOLD_MS = 300000l;

	protected final NHLBot nhlBot;
	
	protected final GameTracker gameTracker;
	
	protected final Guild guild;
	protected final TextChannel channel;
	protected final GuildPreferences preferences;
	protected final GDCMeta meta;

	protected Message introMessage;
	protected Message summaryMessage;
	protected EmbedCreateSpec summaryMessageEmbed; // Used to determine if message needs updating.

	protected AtomicBoolean started = new AtomicBoolean(false);

	protected GameDayThread(NHLBot nhlBot, GameTracker gameTracker, Guild guild, TextChannel channel,
			GuildPreferences preferences, GDCMeta meta) {
		this.nhlBot = nhlBot;
		this.gameTracker = gameTracker;
		this.guild = guild;
		this.channel = channel;
		this.preferences = preferences;
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

		if (!gameTracker.isGameFinished()) {
			initChannel(); // ## Overridable ##

			// Wait until close to start of game
			LOGGER().info("Idling until near game start.");
			waitAndSendReminders();

			// Game is close to starting. Poll at higher rate than previously
			LOGGER().info("Game is about to start. Polling more actively.");
			boolean alreadyStarted = waitForStart();

			// Game has started
			if (!alreadyStarted) {
				LOGGER().info("Game is about to start!");
				sendStartOfGameMessage();
			} else {
				LOGGER().info("Game has already started.");
			}

			while (!gameTracker.isFinished()) {
				try {
					Utils.sleep(ACTIVE_POLL_RATE_MS);
					updateActive(); // ## Overridable ##
				} catch (Exception e) {
					LOGGER().error("Exception occured while running.", e);
				}
			}
			updateEnd(); // ## Overridable ##
		} else {
			LOGGER().info("Game is already finished");
		}
	}

	/*
	 * Run method overrides
	 */	
	protected abstract void setThreadName();

	protected abstract long timeUntilGame();

	protected abstract void initChannel();
	protected abstract void updateActive();
	protected abstract void updateEnd();

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
		do {
			timeTillGameMs = timeUntilGame();
			closeToStart = timeTillGameMs < CLOSE_TO_START_THRESHOLD_MS;
			if (!closeToStart) {
				updateOnReminderWait(); // ## Overridable ##

				// Check to see if message should be sent.
				long lowestThreshold = Long.MAX_VALUE;
				String message = null;
				Iterator<Entry<Long, String>> it = reminders.entrySet().iterator();
				while (it.hasNext()) {
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
				if (message != null && !firstPass) {
					sendMessage(message);
				}
				lowestThreshold = Long.MAX_VALUE;
				message = null;
				firstPass = false;
				LOGGER().trace("Idling until near game start. Sleeping for [" + IDLE_POLL_RATE_MS + "]");

				Utils.sleep(IDLE_POLL_RATE_MS);
			}
		} while (!closeToStart && !isInterrupted());
	}

	protected abstract Map<Long, String> getReminders();

	protected void updateOnReminderWait() {
		return;
	}

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
				Utils.sleep(ACTIVE_POLL_RATE_MS);
			}
		} while (!started && !isInterrupted());
		return alreadyStarted;
	}

	/**
	 * Sends the 'Start of game' message to the game channels of the specified game.
	 * 
	 * @param game
	 *            game of which it's channels will have the messages sent to
	 */
	protected void sendStartOfGameMessage() {
		LOGGER().info("Sending start message.");
		sendMessage(buildStartOfGameMessage());
	}

	protected String buildStartOfGameMessage() {
		List<String> messageList = Arrays.asList(
				"Game is about to start! " + preferences.getCheer(),
				"Be Kind, Be Calm, Be Safe",
				"Be woke, be cool, a calm spirit is smarter.",
				"Get ready, go to the washroom, get your snacks, "
						+ "get your drinks, get your ????, "
						+ "get comfy, and watch us play.",
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

	@Override
	public void process(Event event) {
		// Do Nothing
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
	public static String buildDetailsMessage(Game game) {
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

	/*
	 * Thread Management
	 */
	public void stopAndCleanUp() {
		interrupt();
	}

	@Override
	public void interrupt() {
		super.interrupt();
	}
}
