package com.hazeluff.discord.bot;

import static com.hazeluff.discord.utils.Utils.not;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.command.WordcloudCommand;
import com.hazeluff.discord.bot.database.pole.PollMessage;
import com.hazeluff.discord.bot.database.predictions.campaigns.SeasonCampaign;
import com.hazeluff.discord.bot.database.predictions.campaigns.SeasonCampaign.Prediction;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.listener.IEventProcessor;
import com.hazeluff.discord.nhl.Game;
import com.hazeluff.discord.nhl.GameEvent;
import com.hazeluff.discord.nhl.GameEventStrength;
import com.hazeluff.discord.nhl.GamePeriod;
import com.hazeluff.discord.nhl.GameStatus;
import com.hazeluff.discord.nhl.GameTracker;
import com.hazeluff.discord.nhl.Player;
import com.hazeluff.discord.nhl.Team;
import com.hazeluff.discord.nhl.custommessages.CanucksCustomMessages;
import com.hazeluff.discord.utils.DateUtils;
import com.hazeluff.discord.utils.Utils;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.reaction.Reaction;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.reaction.ReactionEmoji.Unicode;
import discord4j.core.spec.TextChannelCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class GameDayChannel extends Thread implements IEventProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameDayChannel.class);

	// Number of retries to do when NHL API returns no events.
	static final int NHL_EVENTS_RETRIES = 5;

	// Polling time for when game is not close to starting
	static final long IDLE_POLL_RATE_MS = 60000l;
	// Polling time for when game is started/almost-started
	static final long ACTIVE_POLL_RATE_MS = 5000l;
	// Time before game to poll faster
	static final long CLOSE_TO_START_THRESHOLD_MS = 300000l;
	// Time after game is final to continue updates
	static final long POST_GAME_UPDATE_DURATION = 600000l;

	static final Unicode HOME_EMOJI = ReactionEmoji.unicode("🏠");
	static final Unicode AWAY_EMOJI = ReactionEmoji.unicode("✈️");

	// <threshold,message>
	@SuppressWarnings("serial")
	private final Map<Long, String> gameReminders = new HashMap<Long, String>() {
		{
			put(3600000l, "60 minutes till puck drop.");
			put(1800000l, "30 minutes till puck drop.");
			put(600000l, "10 minutes till puck drop.");
		}
	};

	private final NHLBot nhlBot;
	private final GameTracker gameTracker;
	private final Game game;
	private final Guild guild;

	private TextChannel channel;

	private GuildPreferences preferences;

	private List<GameEvent> events = new ArrayList<>();
	private int eventsRetries = 0;

	// Map<eventId, message>
	private final Map<Integer, Pair<GameEvent, Message>> eventMessages = new HashMap<>();

	private Message endOfGameMessage;

	private Message pollMessage;

	private AtomicBoolean started = new AtomicBoolean(false);

	GameDayChannel(NHLBot nhlBot, GameTracker gameTracker, Game game, List<GameEvent> events, Guild guild,
			TextChannel channel) {
		setUncaughtExceptionHandler(new ExceptionHandler(GameDayChannelsManager.class));
		this.nhlBot = nhlBot;
		this.gameTracker = gameTracker;
		this.game = game;
		this.events = events;
		this.guild = guild;
		this.channel = channel;
	}

	GameDayChannel(NHLBot nhlBot, Game game, Guild guild) {
		this(nhlBot, null, game, game.getEvents(), guild, null);
	}

	GameDayChannel(NHLBot nhlBot, GameTracker gameTracker, Guild guild) {
		this(nhlBot, gameTracker, gameTracker.getGame(), gameTracker.getGame().getEvents(), guild, null);
	}

	public static GameDayChannel get(NHLBot nhlBot, GameTracker gameTracker, Guild guild) {
		GameDayChannel gameDayChannel = new GameDayChannel(nhlBot, gameTracker, guild);
		gameDayChannel.createChannel();
		gameDayChannel.start();
		return gameDayChannel;
	}

	/**
	 * Gets a {@link GameDayChannel} object for any game.
	 * 
	 * @param game
	 *            game to get {@link GameDayChannel} for. The game should have teams
	 *            that include the current object's team.
	 */
	private GameDayChannel getGameDayChannel(Game game) {
		return new GameDayChannel(nhlBot, game, guild);
	}

	void updateEvents(List<GameEvent> events) {
		this.events = events;
	}

	/*
	 * Thread
	 */

	@Override
	public void start() {
		if (!started.get()) {
			started.set(true);
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
		String channelName = getChannelName();
		String threadName = String.format("<%s> <%s>", guild.getName(), channelName);
		setName(threadName);
		LOGGER.info("Started thread for channel [{}] in guild [{}]", channelName, guild.getName());

		// Post Predictions poll
		pollMessage = createPredictionPoll();

		if (game.getStatus() != GameStatus.FINAL) {

			// Wait until close to start of game
			LOGGER.info("Idling until near game start.");
			sendReminders();

			// Game is close to starting. Poll at higher rate than previously
			LOGGER.info("Game is about to start. Polling more actively.");
			boolean alreadyStarted = waitForStart();

			// Game has started
			if (!alreadyStarted) {
				LOGGER.info("Game is about to start!");
				sendStartOfGameMessage();
				savePredictions();
			} else {
				LOGGER.info("Game has already started.");
			}

			while (!gameTracker.isFinished()) {
				List<GameEvent> fetchedEvents = game.getEvents();
				if (!isRetryEventFetch(fetchedEvents)) {
					updateMessages(fetchedEvents);
					updateEvents(fetchedEvents);

					if (game.getStatus() == GameStatus.FINAL) {
						updateEndOfGameMessage();
					}
				}
				Utils.sleep(ACTIVE_POLL_RATE_MS);
			}
			sendWordcloud();
		} else {
			LOGGER.info("Game is already finished");
		}

		// Deregister processing on ReactionListener
		unregisterFromListener();
		LOGGER.info("Thread Completed");
	}

	/*
	 * Other Methods
	 */
	void createChannel() {
		String channelName = getChannelName();
		Predicate<TextChannel> channelMatcher = c -> c.getName().equalsIgnoreCase(channelName);
		preferences = nhlBot.getPersistentData().getPreferencesData().getGuildPreferences(guild.getId().asLong());

		Category category = nhlBot.getGdcCategoryManager().get(guild);
		if (!nhlBot.getDiscordManager().getTextChannels(guild).stream().anyMatch(channelMatcher)) {
			Consumer<TextChannelCreateSpec> channelSpec = spec -> {
				spec.setName(channelName);
				spec.setTopic(preferences.getCheer());
				if (category != null) {
					spec.setParentId(category.getId());
				}
			};
			channel = nhlBot.getDiscordManager().createAndGetChannel(guild, channelSpec);
			if (channel != null) {
				preferences.getTimeZone();
				Message message = sendAndGetMessage(getDetailsMessage(preferences.getTimeZone()));
				nhlBot.getDiscordManager().pinMessage(message);
			}
		} else {
			LOGGER.debug("Channel [" + channelName + "] already exists in [" + guild.getName() + "]");
			channel = nhlBot.getDiscordManager().getTextChannels(guild).stream()
					.filter(channelMatcher)
					.findAny()
					.orElse(null);

			if (!channel.getCategoryId().isPresent() && category != null) {
				nhlBot.getDiscordManager().moveChannel(category, channel);
			}
		}
	}

	/**
	 * Stops the thread and deletes the channel from the Discord Guild.
	 */
	void stopAndRemoveGuildChannel() {
		nhlBot.getDiscordManager().deleteChannel(channel);
		interrupt();
	}

	@Override
	public void interrupt() {
		unregisterFromListener();
		super.interrupt();
	}

	/**
	 * Sends reminders of time till the game starts.
	 * 
	 * @throws InterruptedException
	 */
	void sendReminders() {
		boolean firstPass = true;
		boolean closeToStart;
		long timeTillGameMs = Long.MAX_VALUE;
		do {
			timeTillGameMs = DateUtils.diffMs(ZonedDateTime.now(), game.getDate());
			closeToStart = timeTillGameMs < CLOSE_TO_START_THRESHOLD_MS;
			if (!closeToStart) {
				// Check to see if message should be sent.
				long lowestThreshold = Long.MAX_VALUE;
				String message = null;
				Iterator<Entry<Long, String>> it = gameReminders.entrySet().iterator();
				while (it.hasNext()) {
					Entry<Long, String> entry = it.next();
					long threshold = entry.getKey();
					if (threshold > timeTillGameMs) {
						if (lowestThreshold > threshold) {
							lowestThreshold = threshold;
							message = entry.getValue();
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
		boolean alreadyStarted = game.getStatus() != GameStatus.PREVIEW;
		boolean started = false;
		do {
			started = game.getStatus() != GameStatus.PREVIEW;
			if (!started && !isInterrupted()) {
				LOGGER.trace("Game almost started. Sleeping for [" + ACTIVE_POLL_RATE_MS + "]");
				Utils.sleep(ACTIVE_POLL_RATE_MS);
			}
		} while (!started && !isInterrupted());
		return alreadyStarted;
	}

	/**
	 * 
	 * <p>
	 * Posts/Updates/Removes Messages from the Channels based on the state of the
	 * Game's GameEvents.
	 * </p>
	 * 
	 * <p>
	 * Updates the stored events.
	 * </p>
	 * 
	 * @param fetchedEvents
	 *            the new list of events to update to
	 */
	void updateMessages(List<GameEvent> fetchedEvents) {
		fetchedEvents.forEach(retrievedEvent -> {
			if (retrievedEvent.getPlayers().isEmpty()) {
				return;
			}

			GameEvent existingEvent = events.stream().filter(event -> event.getId() == retrievedEvent.getId()).findAny()
					.orElse(null);
			if (existingEvent == null) {
				// New events
				LOGGER.debug("New event: [" + retrievedEvent + "]");
				sendEventMessage(retrievedEvent);
			} else if (!existingEvent.equals(retrievedEvent)) {
				// Updated events
				LOGGER.debug("Updated event: [" + retrievedEvent + "]");
				updateEventMessage(retrievedEvent);
			}
		});

		// Deleted events
		events.forEach(event -> {
			if (fetchedEvents.stream().noneMatch(retrievedEvent -> event.getId() == retrievedEvent.getId())) {
				LOGGER.debug("Removed event: [" + event + "]");
				sendDeletedEventMessage(event);
			}
		});
	}

	/**
	 * <p>
	 * Determines if game events should be fetched again before updating the
	 * messages.
	 * </p>
	 * 
	 * <p>
	 * A retry should happen if the existing events is more than 1 and the api
	 * returned 0 events. Otherwise, if there is 1 existing event and none is
	 * fetched, retry {@link #NHL_EVENTS_RETRIES} times until accepting the changes.
	 * </p>
	 * 
	 * @param fetchedGameEvents
	 *            the fetched game events
	 * @return true - if events should be fetched again<br>
	 *         false - otherwise
	 */
	public boolean isRetryEventFetch(List<GameEvent> fetchedGameEvents) {
		if (fetchedGameEvents.isEmpty()) {
			if (events.size() > 1) {
				LOGGER.warn("NHL api returned no events, but we have stored more than one event.");
				return true;
			} else if (events.size() == 1) {
				LOGGER.warn("NHL api returned no events, but we have stored one event.");
				if (eventsRetries++ < NHL_EVENTS_RETRIES) {
					LOGGER.warn(String.format(
							"Could be a rescinded goal or NHL api issue. " + "Retrying %s time(s) out of %s",
							eventsRetries, NHL_EVENTS_RETRIES));
					return true;
				}
			}
		}
		eventsRetries = 0;
		return false;
	}

	/**
	 * Sends a message with information of the specified event to game channels of
	 * the specified game.
	 * 
	 * @param game
	 *            game of which it's channels will have the messages sent to
	 * @param event
	 *            to create the message from
	 */
	void sendEventMessage(GameEvent event) {
		LOGGER.info("Sending message for event [" + event + "].");
		String strMessage = buildEventMessage(event);
		Message message = sendAndGetMessage(strMessage);
		if (message != null) {
			eventMessages.put(event.getId(), Pair.with(event, message));
		}
	}

	/**
	 * Update previously sent messages of the specified event with the new
	 * information in the event.
	 * 
	 * @param game
	 *            game of which it's channels will have the messages sent to
	 * @param event
	 *            updated event to create the new message from
	 */
	void updateEventMessage(GameEvent event) {
		LOGGER.info("Updating message for event [" + event + "].");
		if (!eventMessages.containsKey(event.getId())) {
			LOGGER.warn("No message exists for the event: {}", event);
		} else {
			Message existingMessage = eventMessages.get(event.getId()).getValue1();
			String oldMsgStr = buildEventMessage(eventMessages.get(event.getId()).getValue0());
			nhlBot.getDiscordManager().deleteMessage(existingMessage);

			String newMsgStr = buildEventMessage(event);
			newMsgStr = String.format(
					"**Goal Updated**\n"
					+ "Previous: ~~%s~~\n"
					+ "%s", 
					oldMsgStr, newMsgStr);
			Message newMessage = nhlBot.getDiscordManager().sendAndGetMessage(channel, newMsgStr);
			eventMessages.put(event.getId(), Pair.with(event, newMessage));
		}
	}

	/**
	 * Sends a message, to indicate specified event was revoked, to game channels of
	 * the specified game.
	 * 
	 * @param game
	 *            game of which it's channels will have the messages sent to
	 * @param event
	 *            the deleted event to create the message from
	 */
	void sendDeletedEventMessage(GameEvent event) {
		LOGGER.info("Sending 'deleted event' message for event [" + event + "].");
		sendMessage(String.format("Goal by %s has been rescinded.", event.getPlayers().get(0).getFullName()));
	}

	/**
	 * Build a message to deliver based on the event.
	 * 
	 * @param event
	 *            event to build message from
	 * @return message to send
	 */
	String buildEventMessage(GameEvent event) {
		GameEventStrength strength = event.getStrength();
		List<Player> players = event.getPlayers();
		StringBuilder message = new StringBuilder();

		// Custom goal message
		String customMessage = CanucksCustomMessages.getMessage(event.getPlayers());
		if (event.getId() % 4 == 0 && customMessage != null) {
			message.append(customMessage).append("\n");
		}

		// Regular message
		if (strength == GameEventStrength.EVEN) {
			message.append(
					String.format("%s goal by **%s**!", event.getTeam().getLocation(), players.get(0).getFullName()));
		} else {
			message.append(String.format("%s %s goal by **%s**!", event.getTeam().getLocation(),
					strength.getValue().toLowerCase(), players.get(0).getFullName()));
		}
		if (players.size() > 1) {
			message.append(String.format(" Assists: %s", players.get(1).getFullName()));
		}
		if (players.size() > 2) {
			message.append(String.format(", %s", players.get(2).getFullName()));
		}
		return message.toString();
	}

	/**
	 * Sends the 'Start of game' message to the game channels of the specified game.
	 * 
	 * @param game
	 *            game of which it's channels will have the messages sent to
	 */
	void sendStartOfGameMessage() {
		LOGGER.info("Sending start message.");
		sendMessage("Game is about to start! " + preferences.getCheer() +
				"\nRemember: Be Kind, Be Calm, Be Safe");
	}

	/**
	 * Updates/Sends the end of game message.
	 */
	void updateEndOfGameMessage() {
		if (endOfGameMessage == null) {
			if (channel != null) {
				endOfGameMessage = sendAndGetMessage(buildEndOfGameMessage());
			}
			if (endOfGameMessage != null) {
				LOGGER.info("Sent end of game message for game. Pinning it...");
				nhlBot.getDiscordManager().pinMessage(endOfGameMessage);
			}
		} else {
			LOGGER.trace("End of game message already sent.");
			String newEndOfGameMessage = buildEndOfGameMessage();
			Message updatedMessage = nhlBot.getDiscordManager()
					.updateAndGetMessage(endOfGameMessage, newEndOfGameMessage);
			if (updatedMessage != null) {
				endOfGameMessage = updatedMessage;
			}
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
		String message = "Game has ended. Thanks for joining!\n" + "Final Score: " + getScoreMessage() + "\n"
				+ "Goals Scored:\n" + getGoalsMessage();

		List<Game> nextGames = preferences.getTeams().stream()
				.map(team -> nhlBot.getGameScheduler().getNextGame(team))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		if (!nextGames.isEmpty()) {
			if (nextGames.size() > 1) {

			} else {
				message += "\nThe next game is: "
						+ getGameDayChannel(nextGames.get(0)).getDetailsMessage(preferences.getTimeZone());
			}
		}
		return message;
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
	 * Gets the date in the format "YY-MM-DD"
	 * 
	 * @param zone
	 *            time zone to convert the time to
	 * @return the date in the format "YY-MM-DD"
	 */
	public String getShortDate(ZoneId zone) {
		return getShortDate(game, zone);
	}

	/**
	 * Gets the date in the format "YY-MM-DD"
	 * 
	 * @param game
	 *            game to get the date from
	 * @param zone
	 *            time zone to convert the time to
	 * @return the date in the format "YY-MM-DD"
	 */
	public static String getShortDate(Game game, ZoneId zone) {
		return game.getDate().withZoneSameInstant(zone).format(DateTimeFormatter.ofPattern("yy-MM-dd"));
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
	public static String getNiceDate(Game game, ZoneId zone) {
		return game.getDate().withZoneSameInstant(zone).format(DateTimeFormatter.ofPattern("EEEE d/MMM/yyyy"));
	}

	/**
	 * Gets the date in the format "EEEE dd MMM yyyy"
	 * 
	 * @param zone
	 *            time zone to convert the time to
	 * @return the date in the format "EEEE dd MMM yyyy"
	 */
	public String getNiceDate(ZoneId zone) {
		return getNiceDate(game, zone);
	}

	/**
	 * Gets the time in the format "HH:mm aaa"
	 * 
	 * @param game
	 * 			  game to get the time from
	 * @param zone
	 *            time zone to convert the time to
	 * @return the time in the format "HH:mm aaa"
	 */
	public static String getTime(Game game, ZoneId zone) {
		return game.getDate().withZoneSameInstant(zone).format(DateTimeFormatter.ofPattern("H:mm z"));
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
	 * @return channel name in format: "AAA_vs_BBB-yy-MM-DD". <br>
	 *         AAA is the 3 letter code of home team<br>
	 *         BBB is the 3 letter code of away team<br>
	 *         yy-MM-DD is a date format
	 */
	public String getChannelName() {
		return getChannelName(game);
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
	public static String getChannelName(Game game) {
		String channelName = String.format("%.3s-vs-%.3s-%s", game.getHomeTeam().getCode(),
				game.getAwayTeam().getCode(), getShortDate(game, ZoneId.of("America/New_York")));
		return channelName.toLowerCase();

	}

	/**
	 * Gets the message that NHLBot will respond with when queried about this game
	 * 
	 * @param game
	 * 		      the game to get the message for
	 * @param timeZone
	 *            the time zone to localize to
	 * 
	 * @return message in the format: "The next game is:\n<br>
	 *         **Home Team** vs **Away Team** at HH:mm aaa on EEEE dd MMM yyyy"
	 */
	public static String getDetailsMessage(Game game, ZoneId timeZone) {
		String message = String.format("**%s** vs **%s** at **%s** on **%s**", game.getHomeTeam().getFullName(),
				game.getAwayTeam().getFullName(), getTime(game, timeZone), getNiceDate(game, timeZone));
		return message;
	}

	/**
	 * Gets the message that NHLBot will respond with when queried about this game
	 * 
	 * @param timeZone
	 *            the time zone to localize to
	 * 
	 * @return message in the format: "The next game is:\n<br>
	 *         **Home Team** vs **Away Team** at HH:mm aaa on EEEE dd MMM yyyy"
	 */
	public String getDetailsMessage(ZoneId timeZone) {
		return getDetailsMessage(game, timeZone);
	}

	/**
	 * Gets the message that NHLBot will respond with when queried about the score
	 * of the game
	 * 
	 * @return message in the format : "Home Team **homeScore** - **awayScore** Away
	 *         Team"
	 */
	public String getScoreMessage() {
		return getScoreMessage(game);
	}
	
	/**
	 * Gets the message that NHLBot will respond with when queried about the score
	 * of the game
	 * 
	 * @param game
	 * 		The game to get the score message of
	 * @return message in the format : "Home Team **homeScore** - **awayScore** Away
	 *         Team"
	 */
	public static String getScoreMessage(Game game) {
		return String.format("%s **%s** - **%s** %s", game.getHomeTeam().getName(), game.getHomeScore(),
				game.getAwayScore(), game.getAwayTeam().getName());
	}

	public String getGoalsMessage() {
		return getGoalsMessage(game);
	}

	public static String getGoalsMessage(Game game) {
		List<GameEvent> goals = game.getEvents();
		StringBuilder response = new StringBuilder();
		response.append("```\n");
		for (int i = 1; i <= 3; i++) {
			switch (i) {
			case 1:
				response.append("1st Period:");
				break;
			case 2:
				response.append("\n\n2nd Period:");
				break;
			case 3:
				response.append("\n\n3rd Period:");
				break;
			}
			int period = i;
			Predicate<GameEvent> isPeriod = gameEvent -> gameEvent.getPeriod().getPeriodNum() == period;
			if (goals.stream().anyMatch(isPeriod)) {
				for (GameEvent gameEvent : goals.stream().filter(isPeriod).collect(Collectors.toList())) {
					response.append("\n").append(gameEvent.getDetails());
				}
			} else {
				response.append("\nNone");
			}
		}
		Predicate<GameEvent> isOtherPeriod = gameEvent -> gameEvent.getPeriod().getPeriodNum() > 3;
		if (goals.stream().anyMatch(isOtherPeriod)) {
			GameEvent gameEvent = goals.stream().filter(isOtherPeriod).findFirst().get();
			GamePeriod period = gameEvent.getPeriod();
			response.append("\n\n").append(period.getDisplayValue()).append(":");
			goals.stream().filter(isOtherPeriod).forEach(event -> response.append("\n").append(event.getDetails()));
		}
		response.append("\n```");
		return response.toString();
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

	protected void sendMessage(String message) {
		if (channel != null) {
			nhlBot.getDiscordManager().sendMessage(channel, message);
		}
	}

	protected Message sendAndGetMessage(String message) {
		return channel == null ? null
				: nhlBot.getDiscordManager().sendAndGetMessage(channel, message);
	}

	static List<Team> getRelevantTeams(List<Team> teams, Game game) {
		return teams.stream().filter(team -> game.containsTeam(team)).collect(Collectors.toList());
	}

	/*
	 * Predictions related
	 */
	private void registerToListener() {
		nhlBot.getReactionListener().addProccessor(this, ReactionAddEvent.class);
		nhlBot.getReactionListener().addProccessor(this, ReactionRemoveEvent.class);
	}

	private void unregisterFromListener() {
		nhlBot.getReactionListener().removeProccessor(this);
	}

	@Override
	public void process(Event event) {
		if (channel == null || pollMessage == null) {
			return;
		}
		
		// Only allow if reaction is done before the start of the game
		if (ZonedDateTime.now().isBefore(game.getDate()) ) {
			return;
		}
		
		if (event instanceof ReactionAddEvent) {
			ReactionAddEvent addEvent = (ReactionAddEvent) event;

			if (!addEvent.getChannelId().equals(channel.getId())) {
				// Not the same channel/game
				return;
			}
			
			Unicode addedUnicodeEmoji = addEvent.getEmoji().asUnicodeEmoji().orElse(null);
			if (addedUnicodeEmoji == null) {
				return;
			}

			String campaignId = SeasonCampaign.buildCampaignId(Config.CURRENT_SEASON.getAbbreviation());
			long userId = addEvent.getUserId().asLong();

			if (addedUnicodeEmoji.equals(HOME_EMOJI)) {
				SeasonCampaign.savePrediction(nhlBot,
						new Prediction(campaignId, userId, game.getGamePk(), game.getHomeTeam().getId()));
				removeReactions(pollMessage, addedUnicodeEmoji, addEvent.getUserId());
			} else if (addedUnicodeEmoji.equals(AWAY_EMOJI)) {
				SeasonCampaign.savePrediction(nhlBot,
						new Prediction(campaignId, userId, game.getGamePk(), game.getAwayTeam().getId()));
				removeReactions(pollMessage, addedUnicodeEmoji, addEvent.getUserId());
			} else {
				LOGGER.warn("Unknown emoji: " + addedUnicodeEmoji);
			}
		} else if (event instanceof ReactionRemoveEvent) {
			ReactionRemoveEvent removeEvent = (ReactionRemoveEvent) event;

			if (!removeEvent.getChannelId().equals(channel.getId())) {
				// Not the same channel/game
				return;
			}

			Unicode removedUnicodeEmoji = removeEvent.getEmoji().asUnicodeEmoji().orElse(null);
			if (removedUnicodeEmoji == null) {
				return;
			}

			String campaignId = SeasonCampaign.buildCampaignId(Config.CURRENT_SEASON.getAbbreviation());
			long userId = removeEvent.getUserId().asLong();
			
			// Do not interact with persistent data if removed emoji was not of the stored prediction.
			// Prevents removing the prediction when NHLBot removes the reaction.
			SeasonCampaign.Prediction prediction = SeasonCampaign.loadPrediction(
					nhlBot, campaignId, game.getGamePk(), userId);

			if (prediction != null) {
				if (removedUnicodeEmoji.equals(HOME_EMOJI)) {
					if (!Integer.valueOf(game.getHomeTeam().getId()).equals(prediction.getPrediction())) {
						return;
					}
				} else if (removedUnicodeEmoji.equals(AWAY_EMOJI)) {
					if (!Integer.valueOf(game.getAwayTeam().getId()).equals(prediction.getPrediction())) {
						return;
					}
				} else {
					LOGGER.warn("Unknown emoji: " + removedUnicodeEmoji);
				}
			}

			// Remove the prediction from the persistent data
			SeasonCampaign.savePrediction(nhlBot, new Prediction(campaignId, userId, game.getGamePk(), null));
		} else {
			LOGGER.warn("Event provided is of unknown type: " + event.getClass().getSimpleName());
		}
	}

	/**
	 * 
	 * @param excludedReaction
	 * @param userId
	 */
	private void removeReactions(Message message, Unicode excludedReaction, Snowflake userId) {
		for (Reaction messageReaction : message.getReactions()) {
			Unicode messageReactionUnicode = messageReaction.getEmoji().asUnicodeEmoji().orElse(null);
			if (messageReactionUnicode != null && !messageReactionUnicode.equals(excludedReaction)) {
				subscribe(message.removeReaction(messageReactionUnicode, userId));
			}
		}
	}

	private Message createPredictionPoll() {
		String pollId = "gameday-" + getChannelName();
		PollMessage poll = nhlBot.getPersistentData().getPolesData().loadPoll(channel.getId().asLong(), pollId);
		Message message;
		if (poll == null) {
			message = sendPredictionMessage();

			poll = PollMessage.of(channel.getId().asLong(), message.getId().asLong(), pollId);

			// Save pole to database
			nhlBot.getPersistentData().getPolesData().savePoll(poll);
		} else {
			message = nhlBot.getDiscordManager().getMessage(poll.getChannelId(), poll.getMessageId());
			if (message == null) {
				message = sendPredictionMessage();
			}
			nhlBot.getDiscordManager().pinMessage(message);
		}

		registerToListener();

		return message;
	}

	private void savePredictions() {
		LOGGER.info("Saving Predictions: channel={}, pollMessage={}", channel, pollMessage);
		if (channel != null && pollMessage != null) {
			String campaignId = SeasonCampaign.buildCampaignId(Config.CURRENT_SEASON.getAbbreviation());
			// Save Home Predictors
			block(pollMessage.getReactors(HOME_EMOJI)).stream().filter(not(this::isBotSelf))
					.forEach(user -> SeasonCampaign.savePrediction(nhlBot, new Prediction(campaignId,
							user.getId().asLong(), game.getGamePk(), game.getHomeTeam().getId())));
			// Save Away Predictors
			block(pollMessage.getReactors(AWAY_EMOJI)).stream().filter(not(this::isBotSelf))
					.forEach(user -> SeasonCampaign.savePrediction(nhlBot, new Prediction(campaignId,
							user.getId().asLong(), game.getGamePk(), game.getAwayTeam().getId())));

		}
	}

	private Message sendPredictionMessage() {
		String pollMessage = String.format("Predict the outcome of this game!\n🏠 %s\n✈️  %s",
				game.getHomeTeam().getFullName(), game.getAwayTeam().getFullName());

		Message message = sendAndGetMessage(pollMessage);
		subscribe(message.addReaction(HOME_EMOJI));
		subscribe(message.addReaction(AWAY_EMOJI));
		subscribe(message.pin());

		return message;
	}

	private void sendWordcloud() {
		new WordcloudCommand(nhlBot).sendWordcloud(channel, game, preferences.getTimeZone());
	}

	boolean isBotSelf(User user) {
		return user.getId().equals(nhlBot.getDiscordManager().getId());
	}

	<T> T block(Mono<T> mono) {
		return nhlBot.getDiscordManager().block(mono);
	}

	<T> List<T> block(Flux<T> flux) {
		return nhlBot.getDiscordManager().block(flux);
	}

	void subscribe(Mono<Void> mono) {
		nhlBot.getDiscordManager().subscribe(mono);
	}

	void subscribe(Flux<?> flux) {
		nhlBot.getDiscordManager().subscribe(flux);
	}
}
