package com.hazeluff.discord.bot.gdc;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.gdc.GDCGoalsCommand;
import com.hazeluff.discord.bot.command.gdc.GDCScoreCommand;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMeta;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.bot.listener.IEventProcessor;
import com.hazeluff.discord.nhl.GameTracker;
import com.hazeluff.discord.utils.DateUtils;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.Team;
import com.hazeluff.nhl.event.GoalEvent;
import com.hazeluff.nhl.event.PenaltyEvent;
import com.hazeluff.nhl.game.Game;

import discord4j.core.event.domain.Event;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.core.spec.TextChannelCreateSpec;

public class FourNationsGameDayThread extends Thread implements IEventProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(FourNationsGameDayThread.class);

	// Number of retries to do when NHL API returns no events.
	static final int NHL_EVENTS_RETRIES = 5;

	// Time to wait before tryign to fetch the Discord channel
	static final long CHANNEL_FETCH_RETRY_RATE_MS = 60000l;
	// Polling time for when game is not close to starting
	static final long IDLE_POLL_RATE_MS = 60000l;
	// Polling time for when game is started/almost-started
	static final long ACTIVE_POLL_RATE_MS = 10000l;
	// Time before game to poll faster
	static final long CLOSE_TO_START_THRESHOLD_MS = 300000l;
	// Time after game is final to continue updates
	static final long POST_GAME_UPDATE_DURATION = 600000l;

	// Message Managers
	static final long SPAM_COOLDOWN_MS = 30000l; // Applies only to custom messages
	private final GoalMessagesManager goalMessages;
	private final PenaltyMessagesManager penaltyMessages;

	// <threshold,message>
	@SuppressWarnings("serial")
	private final Map<Long, String> gameReminders = new HashMap<Long, String>() {
		{
			put(3600000l, "60 minutes till puck drop.");
		}
	};

	private final NHLBot nhlBot;
	private final GameTracker gameTracker;
	private final Game game;
	private final Guild guild;
	private final TextChannel channel;
	@SuppressWarnings("unused")
	private final GuildPreferences preferences;
	private final GDCMeta meta;

	private Message summaryMessage;
	private EmbedCreateSpec summaryMessageEmbed; // Used to determine if message needs updating.

	private AtomicBoolean started = new AtomicBoolean(false);

	private FourNationsGameDayThread(NHLBot nhlBot, GameTracker gameTracker, Guild guild, TextChannel channel,
			GuildPreferences preferences, GDCMeta meta) {
		this.nhlBot = nhlBot;
		this.gameTracker = gameTracker;
		this.game = gameTracker.getGame();
		this.guild = guild;
		this.channel = channel;
		this.preferences = preferences;
		this.meta = meta;
		this.goalMessages = new GoalMessagesManager(SPAM_COOLDOWN_MS, nhlBot, game, channel, meta);
		this.penaltyMessages = new PenaltyMessagesManager(nhlBot, game, channel, meta);

	}

	public static FourNationsGameDayThread get(NHLBot nhlBot, TextChannel textChannel, GameTracker gameTracker, Guild guild) {
		GuildPreferences preferences = nhlBot.getPersistentData().getPreferencesData()
				.getGuildPreferences(guild.getId().asLong());
		GDCMeta meta = null;
		if (textChannel != null) {
			meta = nhlBot.getPersistentData().getGDCMetaData().loadMeta(
				textChannel.getId().asLong(),
				gameTracker.getGame().getGameId()
			);
			if (meta == null) {
				meta = GDCMeta.of(textChannel.getId().asLong(), gameTracker.getGame().getGameId());
			}
		}
		FourNationsGameDayThread gameDayChannel = new FourNationsGameDayThread(nhlBot, gameTracker, guild,
				textChannel, preferences, meta);

		if (gameDayChannel.channel != null) {
			gameDayChannel.loadMetadata();
			gameDayChannel.start();
		} else {
			LOGGER.warn("GameDayChannel not started. TextChannel could not be found. guild={}", guild.getId().asLong());
		}
		return gameDayChannel;
	}

	static TextChannel getTextChannel(Guild guild, Game game, NHLBot nhlBot, GuildPreferences preferences) {
		TextChannel channel = null;
		try {
			String channelName = buildChannelName(game);
			Predicate<TextChannel> channelMatcher = c -> c.getName().equalsIgnoreCase(channelName);
			Category category = nhlBot.getGdcCategoryManager().get(guild);
			if (!DiscordManager.getTextChannels(guild).stream().anyMatch(channelMatcher)) {
				TextChannelCreateSpec.Builder channelSpecBuilder = TextChannelCreateSpec.builder();
				channelSpecBuilder.name(channelName);
				channelSpecBuilder.topic(preferences.getCheer());
				if (category != null) {
					channelSpecBuilder.parentId(category.getId());
				}
				channel = DiscordManager.createAndGetChannel(guild, channelSpecBuilder.build());
			} else {
				LOGGER.debug("Channel [" + channelName + "] already exists in [" + guild.getName() + "]");
				channel = DiscordManager.getTextChannels(guild).stream().filter(channelMatcher).findAny().orElse(null);

				if (category != null && !channel.getCategoryId().isPresent()) {
					DiscordManager.moveChannel(category, channel);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Failed to create channel.", e);
		}
		return channel;
	}

	/*
	 * Metadata
	 */
	private void loadMetadata() {
		LOGGER.trace("Load Metadata.");
		// Load Goal Messages
		goalMessages.initEventMessages(meta.getGoalMessageIds());
		// Load Penalty Messages
		penaltyMessages.initEventMessages(meta.getPenaltyMessageIds());

		saveMetadata();
	}

	private void saveMetadata() {
		nhlBot.getPersistentData().getGDCMetaData().save(meta);
	}

	/*
	 * Thread
	 */

	@Override
	public void start() {
		if (started.compareAndSet(false, true)) {
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
			_run();
		} catch (Exception e) {
			LOGGER.error("Error occurred while running thread.", e);
		} finally {
			LOGGER.info("Thread completed");
		}
	}

	private void _run() {
		String serverName = guild.getName();
		String gameName = buildChannelName(this.game);
		String threadName = String.format("<%s> <%s>", serverName, gameName);
		setName(threadName);
		LOGGER.info("Started Four Nations GDC thread.");

		this.goalMessages.initEvents(game.getScoringEvents());
		this.penaltyMessages.initEvents(game.getPenaltyEvents());
		
		updateSummaryMessage();

		if (!game.getGameState().isFinished()) {
			// Wait until close to start of game
			LOGGER.info("Idling until near game start.");
			if (!game.isStartTimeTBD()) {
				waitAndSendReminders();
			}

			// Game is close to starting. Poll at higher rate than previously
			LOGGER.info("Game is about to start. Polling more actively.");
			boolean alreadyStarted = waitForStart();

			// Game has started
			if (!alreadyStarted) {
				LOGGER.info("Game is about to start!");
				sendStartOfGameMessage();
			} else {
				LOGGER.info("Game has already started.");
			}

			while (!gameTracker.isFinished()) {
				try {
					Utils.sleep(ACTIVE_POLL_RATE_MS);

					updateMessages();
					updateSummaryMessage();
					EmbedCreateSpec newSummaryMessageEmbed = getSummaryEmbedSpec();
					boolean updatedSummary = !newSummaryMessageEmbed.equals(summaryMessageEmbed);
					if (summaryMessage != null && updatedSummary) {
						updateSummaryMessage(newSummaryMessageEmbed);
					}
				} catch (Exception e) {
					LOGGER.error("Exception occured while running.", e);
				}
			}
			sendEndOfGameMessage();
		} else {
			LOGGER.info("Game is already finished");
		}
	}

	/**
	 * Used to update all messages/pins.
	 * 
	 * @throws LiveDataException
	 */
	public void refresh() {
		try {
			gameTracker.updateGame();
			updateMessages();
			updateSummaryMessage(getSummaryEmbedSpec());
		} catch (Exception e) {
			LOGGER.error("Exception occured while refreshing.", e);
		}
	}

	private void updateMessages() {
		try {
			List<GoalEvent> goalEvents = game.getScoringEvents();
			List<PenaltyEvent> penaltyEvents = game.getPenaltyEvents();
			goalMessages.updateMessages(goalEvents);
			penaltyMessages.updateMessages(penaltyEvents);
		} catch (Exception e) {
			LOGGER.error("Exception occured while updating messages.", e);
		}
	}

	/**
	 * Waits for the game to start and sends reminders of time till it starts.
	 * 
	 * @throws InterruptedException
	 */
	void waitAndSendReminders() {
		boolean firstPass = true;
		boolean closeToStart;
		long timeTillGameMs = Long.MAX_VALUE;
		do {
			timeTillGameMs = DateUtils.diffMs(ZonedDateTime.now(), game.getStartTime());
			closeToStart = timeTillGameMs < CLOSE_TO_START_THRESHOLD_MS;
			if (!closeToStart) {
				// Update Summary Message
				if (summaryMessage == null) {
					updateSummaryMessage();
				}

				// Check to see if reminder message should be sent.
				long lowestThreshold = Long.MAX_VALUE;
				String message = null;
				Iterator<Entry<Long, String>> it = gameReminders.entrySet().iterator();
				while (it.hasNext()) {
					Entry<Long, String> entry = it.next();
					long threshold = entry.getKey();
					if (threshold > timeTillGameMs) {
						if (lowestThreshold > threshold) {
							lowestThreshold = threshold;
							message = getFourNationsMatchupName() + ": " + entry.getValue();
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
				LOGGER.trace("Idling until near game start. Sleeping for [" + IDLE_POLL_RATE_MS + "]");

				Utils.sleep(IDLE_POLL_RATE_MS);
			}
		} while (!closeToStart && !isInterrupted());
	}

	/**
	 * Polls at higher polling rate before game starts. Returns whether or not the
	 * game has already started
	 * 
	 * @return true, if game is already started<br>
	 *         false, otherwise
	 * @throws InterruptedException
	 */
	boolean waitForStart() {
		boolean alreadyStarted = game.getGameState().isStarted();
		boolean started = false;
		do {
			started = game.getGameState().isStarted();
			if (!started && !isInterrupted()) {
				LOGGER.trace("Game almost started. Sleeping for [" + ACTIVE_POLL_RATE_MS + "]");
				Utils.sleep(ACTIVE_POLL_RATE_MS);
			}
		} while (!started && !isInterrupted());
		return alreadyStarted;
	}

	/**
	 * Sends the 'Start of game' message to the channel.
	 * 
	 * @param game
	 *            game of which it's channels will have the messages sent to
	 */
	void sendStartOfGameMessage() {
		LOGGER.info("Sending start message.");
		String message = String.format("%s: \n", getFourNationsMatchupName());
		message += "Game is about to start!";
		sendMessage(message);
	}

	/*
	 * Summary Message
	 */
	public void updateSummaryMessage() {
		LOGGER.info("Update Summary Message.");
		if (Duration.between(ZonedDateTime.now(), game.getStartTime()).toDays() < 4) {
			summaryMessage = getSummaryMessage();
		}
	}

	private Message getSummaryMessage() {
		Message message = null;
		if (meta != null) {
			Long messageId = meta.getSummaryMessageId();
			if (messageId == null) {
				// No message saved
				message = sendSummaryMessage();
			} else {
				message = nhlBot.getDiscordManager().getMessage(channel.getId().asLong(), messageId);
				if (message == null) {
					// Could not find existing message. Send new message
					message = sendSummaryMessage();
				} else {
					// Message exists
					return message;
				}
			}

			if (message != null) {
				DiscordManager.pinMessage(message);
				meta.setSummaryMessageId(message.getId().asLong());
				saveMetadata();
			}
		}
		return message;
	}

	private Message sendSummaryMessage() {
		this.summaryMessageEmbed = getSummaryEmbedSpec();
		MessageCreateSpec messageSpec = MessageCreateSpec.builder().addEmbed(summaryMessageEmbed).build();
		return DiscordManager.sendAndGetMessage(channel, messageSpec);
	}

	private void updateSummaryMessage(EmbedCreateSpec newSummaryMessageEmbed) {
		this.summaryMessageEmbed = newSummaryMessageEmbed;
		MessageEditSpec messageSpec = MessageEditSpec.builder().addEmbed(summaryMessageEmbed).build();
		DiscordManager.updateMessage(summaryMessage, messageSpec);
	}

	private EmbedCreateSpec getSummaryEmbedSpec() {
		EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();
		embedBuilder.addField("Four Nations", buildDetailsMessage(getGame()), false);
		GDCScoreCommand.buildEmbed(embedBuilder, game);
		GDCGoalsCommand.buildEmbed(embedBuilder, game);
		return embedBuilder.build();
	}

	/*
	 * End of game message
	 */
	/**
	 * Sends the end of game message.
	 */
	void sendEndOfGameMessage() {
		try {
			if (channel != null) {
				DiscordManager.sendAndGetMessage(channel, buildEndOfGameMessage());
			}
		} catch (Exception e) {
			LOGGER.error("Could not send end of game Message.");
		}
	}

	/**
	 * Builds the message that is sent at the end of the game.
	 * 
	 * @param game
	 *            the game to build the message for
	 * @param team
	 *            team to specialize the message for
	 * @return end of game message
	 */
	String buildEndOfGameMessage() {
		String message = String.format("**%s**\n", getFourNationsMatchupName());
		message += "Game has ended. Thanks for joining!\n" + "Final Score: " + buildGameScore(game);
		return message;
	}

	private static String buildGameScore(Game game) {
		return String.format("%s **%s** - **%s** %s", game.getHomeTeam().getName(), game.getHomeScore(),
				game.getAwayScore(), game.getAwayTeam().getName());
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

	public Game getGame() {
		return game;
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
	public static String buildChannelDate(Game game, ZoneId zone) {
		return game.getStartTime().withZoneSameInstant(zone).format(DateTimeFormatter.ofPattern("yy-MM-dd"));
	}

	/**
	 * Gets the date in the format "EEEE dd MMM yyyy"
	 * 
	 * @param game
	 *            game to get the date for
	 * @param zone
	 *            time zone to convert the time to
	 * @return the date in the format "EEEE dd MMM yyyy"
	 */
	public static String buildNiceDate(Game game, ZoneId zone) {
		return game.getStartTime().withZoneSameInstant(zone).format(DateTimeFormatter.ofPattern("EEEE, d/MMM/yyyy"));
	}

	/**
	 * Gets the date in the format "EEEE dd MMM yyyy"
	 * 
	 * @param zone
	 *            time zone to convert the time to
	 * @return the date in the format "EEEE dd MMM yyyy"
	 */
	public String getNiceDate(ZoneId zone) {
		return buildNiceDate(game, zone);
	}

	/**
	 * Gets the time in the format "HH:mm aaa"
	 * 
	 * @param game
	 *            game to get the time from
	 * @param zone
	 *            time zone to convert the time to
	 * @return the time in the format "HH:mm aaa"
	 */
	public static String getTime(Game game, ZoneId zone) {
		return game.getStartTime().withZoneSameInstant(zone).format(DateTimeFormatter.ofPattern("H:mm z"));
	}

	/**
	 * Gets the time in the format "HH:mm aaa"
	 * 
	 * @param zone
	 *            time zone to convert the time to
	 * @return the time in the format "HH:mm aaa"
	 */
	public String getTime(ZoneId zone) {
		return getTime(game, zone);
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
	public static String buildChannelName(Game game) {
		String channelName = String.format("%.3s-vs-%.3s-%s", game.getHomeTeam().getCode(),
				game.getAwayTeam().getCode(), buildChannelDate(game, Config.ZONE_ID));
		return channelName.toLowerCase();
	}

	public static String buildFourNationsMatchupName(Game game) {
		return String.format(
				"**%s** vs **%s**", 
				game.getHomeTeam().getLocation(), game.getAwayTeam().getLocation()
			);
	}

	String getFourNationsMatchupName() {
		return buildFourNationsMatchupName(getGame());
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
				game.getHomeTeam().getLocation(), game.getAwayTeam().getLocation(), 
				time
			);
		return message;
	}

	/**
	 * Determines if the given channel name is that of a possible game. Does not
	 * factor into account whether or not the game is real.
	 * 
	 * @param channelName
	 *            name of the channel
	 * @return true, if is of game channel format;<br>
	 *         false, otherwise.
	 */
	public static boolean isChannelNameFormat(String channelName) {
		String teamRegex = String.join("|", Arrays.asList(Team.values()).stream()
				.map(team -> team.getCode().toLowerCase()).collect(Collectors.toList()));
		teamRegex = String.format("(%s)", teamRegex);
		String regex = String.format("%1$s-vs-%1$s-[0-9]{2}-[0-9]{2}-[0-9]{2}", teamRegex);
		return channelName.matches(regex);
	}

	/*
	 * Thread Management
	 */
	/**
	 * Stops the thread.
	 */
	void stopChannel() {
		interrupt();
	}

	@Override
	public void interrupt() {
		super.interrupt();
	}
}
