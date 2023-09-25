package com.hazeluff.discord.bot.gdc;

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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.WordcloudCommand;
import com.hazeluff.discord.bot.command.gdc.GDCGoalsCommand;
import com.hazeluff.discord.bot.command.gdc.GDCScoreCommand;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMeta;
import com.hazeluff.discord.bot.database.predictions.campaigns.SeasonCampaign;
import com.hazeluff.discord.bot.database.predictions.campaigns.SeasonCampaign.Prediction;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.bot.gdc.custom.CustomMessages;
import com.hazeluff.discord.bot.listener.IEventProcessor;
import com.hazeluff.discord.nhl.GameTracker;
import com.hazeluff.discord.utils.DateUtils;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.Team;
import com.hazeluff.nhl.event.GoalEvent;
import com.hazeluff.nhl.event.PenaltyEvent;
import com.hazeluff.nhl.game.Game;
import com.hazeluff.nhl.game.RosterPlayer;

import discord4j.core.event.domain.Event;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.reaction.ReactionEmoji.Unicode;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.core.spec.TextChannelCreateSpec;
import discord4j.rest.util.Color;

public class GameDayChannel extends Thread implements IEventProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameDayChannel.class);

	// Number of retries to do when NHL API returns no events.
	static final int NHL_EVENTS_RETRIES = 5;

	// Time to wait before tryign to fetch the Discord channel
	static final long CHANNEL_FETCH_RETRY_RATE_MS = 60000l;
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

	private GDCMeta meta;

	private List<GoalEvent> cachedGoalEvents = new ArrayList<>();
	private List<PenaltyEvent> cachedPenaltyEvents = new ArrayList<>();

	// Events that happened before the bot started.
	private List<GoalEvent> skippableGoalEvents = new ArrayList<>();
	private List<PenaltyEvent> skippablePenaltyEvents = new ArrayList<>();

	private final Map<Integer, Message> goalEventMessages = new HashMap<>();
	private final Map<Integer, Message> penaltyEventMessages = new HashMap<>();

	private Message summaryMessage;
	private Message votingMessage;
	private EmbedCreateSpec summaryMessageEmbed; // Used to determine if message needs updating.
	private Message endOfGameMessage;

	private AtomicBoolean started = new AtomicBoolean(false);

	private GameDayChannel(NHLBot nhlBot, GameTracker gameTracker,
			Game game, Guild guild, TextChannel channel) {
		this.nhlBot = nhlBot;
		this.gameTracker = gameTracker;
		this.game = game;
		this.guild = guild;
		this.channel = channel;
	}

	GameDayChannel(NHLBot nhlBot, Game game, Guild guild) {
		this(nhlBot, null, game, guild, null);
	}

	GameDayChannel(NHLBot nhlBot, GameTracker gameTracker, Guild guild) {
		this(nhlBot, gameTracker, gameTracker.getGame(), guild, null);
	}

	public static GameDayChannel get(NHLBot nhlBot, GameTracker gameTracker, Guild guild) {
		GameDayChannel gameDayChannel = new GameDayChannel(nhlBot, gameTracker, guild);
		gameDayChannel.channel = gameDayChannel.getChannel();
		gameDayChannel.loadMetadata();
		gameDayChannel.start();
		return gameDayChannel;
	}

	TextChannel getChannel() {
		TextChannel channel = null;
		try {
			String channelName = getChannelName();
			Predicate<TextChannel> channelMatcher = c -> c.getName().equalsIgnoreCase(channelName);
			preferences = nhlBot.getPersistentData().getPreferencesData().getGuildPreferences(guild.getId().asLong());
			Category category = nhlBot.getGdcCategoryManager().get(guild);
			if (!DiscordManager.getTextChannels(guild).stream().anyMatch(channelMatcher)) {
				TextChannelCreateSpec.Builder channelSpecBuilder = TextChannelCreateSpec.builder();
				channelSpecBuilder.name(channelName);
				channelSpecBuilder.topic(preferences.getCheer());
				if (category != null) {
					channelSpecBuilder.parentId(category.getId());
				}
				channel = DiscordManager.createAndGetChannel(guild, channelSpecBuilder.build());
				if (channel != null) {
					// Send Messages to Initialize Channel
					sendDetailsMessage(channel);
				}
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

	/*
	 * Metadata
	 */
	private void loadMetadata() {
		LOGGER.trace("Load Metadata.");
		if (channel != null) {
			meta = nhlBot.getPersistentData().getGDCMetaData().loadMeta(channel.getId().asLong());
			if (meta == null) {
				meta = GDCMeta.of(channel.getId().asLong());
			}
			// Load Goal Messages
			addEventMessages(goalEventMessages, meta.getGoalMessageIds());
			// Load Penalty Messages
			addEventMessages(penaltyEventMessages, meta.getPenaltyMessageIds());

			saveMetadata();
		}
	}

	private void addEventMessages(Map<Integer, Message> map, Map<Integer, Long> eventMessages) {
		for (Entry<Integer, Long> entry : eventMessages.entrySet()) {
			Integer eventId = entry.getKey();
			Long messageId = entry.getValue();
			Message message = nhlBot.getDiscordManager().getMessage(channel.getId().asLong(), messageId);
			if (message != null) {
				map.put(eventId, message);
			} else {
				LOGGER.warn("Failed to find message: " + messageId);
			}
		}
	}


	private void saveMetadata() {
		LOGGER.trace("Save Metadata.");
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
			// Deregister processing on ReactionListener
			// unregisterFromListener();
			LOGGER.info("Thread completed");
		}
	}

	private void _run() {
		String channelName = getChannelName();
		String threadName = String.format("<%s> <%s>", guild.getName(), channelName);
		setName(threadName);
		LOGGER.info("Started GameDayChannel thread.");

		this.skippableGoalEvents = game.getScoringEvents();
		this.skippablePenaltyEvents = game.getPenaltyEvents();

		this.cachedGoalEvents = game.getScoringEvents();
		this.cachedPenaltyEvents = game.getPenaltyEvents();

		// Post Predictions poll
		summaryMessage = getSummaryMessage();
		votingMessage = getVotingMessage();

		if (!game.getGameState().isFinal()) {
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
			} else {
				LOGGER.info("Game has already started.");
			}

			while (!gameTracker.isFinished()) {
				updateMessages();

				EmbedCreateSpec newSummaryMessageEmbed = getSummaryEmbedSpec();
				boolean updatedSummary = !newSummaryMessageEmbed.equals(summaryMessageEmbed);
				if (summaryMessage != null && updatedSummary) {
					updateSummaryMessage(newSummaryMessageEmbed);
				}

				if (game.getGameState().isFinal()) {
					updateEndOfGameMessage();
				}
				Utils.sleep(ACTIVE_POLL_RATE_MS);
			}

			sendWordcloud();
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
		gameTracker.updateGame();
		updateMessages();
		updateSummaryMessage(getSummaryEmbedSpec());
	}

	private void updateMessages() {
		List<GoalEvent> goalEvents = game.getScoringEvents();
		List<PenaltyEvent> penaltyEvents = game.getPenaltyEvents();
		updateGoalMessages(goalEvents);
		updatePenaltyMessages(penaltyEvents);
		this.cachedGoalEvents = goalEvents;
		this.cachedPenaltyEvents = penaltyEvents;
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
			timeTillGameMs = DateUtils.diffMs(ZonedDateTime.now(), game.getStartTime());
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
	 * Sends the 'Start of game' message to the game channels of the specified game.
	 * 
	 * @param game
	 *            game of which it's channels will have the messages sent to
	 */
	void sendStartOfGameMessage() {
		LOGGER.info("Sending start message.");
		sendMessage("Game is about to start! " + preferences.getCheer() + "\nRemember: Be Kind, Be Calm, Be Safe");
	}

	/*
	 * Goals
	 */
	public GoalEvent getSkippableGoalEvent(int id) {
		return skippableGoalEvents.stream()
				.filter(skippableGoalEvent -> skippableGoalEvent.getId() == id)
				.findAny()
				.orElse(null);
	}

	public GoalEvent getCachedGoalEvent(int id) {
		return cachedGoalEvents.stream()
				.filter(cachedGoalEvent -> cachedGoalEvent.getId() == id)
				.findAny()
				.orElse(null);
	}
	
	/**
	 * <p>
	 * Sends/Updates messages for GoalEvents. If message has not been sent, it will
	 * be created. Otherwise, the existing message will be updated.
	 * </p>
	 * 
	 * <p>
	 * Does not update for events occurring before this thread (/the bot) was
	 * started.
	 * </p>
	 */
	private void updateGoalMessages(List<GoalEvent> goals) {
		// Update Messages
		goals.forEach(currentEvent -> {
			GoalEvent skippableEvent = getSkippableGoalEvent(currentEvent.getId());
			if (skippableEvent != null) {
				return;
			}
			if (!goalEventMessages.containsKey(currentEvent.getId())) {
				// New event
				sendGoalMessage(currentEvent);
			} else if (isGoalEventUpdated(currentEvent)) {
				// Updated event
				updateGoalMessage(currentEvent);
			}
		});

		// Remove Messages that do not have a corresponding event in the game's data.
		goalEventMessages.entrySet().removeIf(entry -> {
			int eventId = entry.getKey().intValue();
			if (goals.stream().noneMatch(currentEvent -> currentEvent.getId() == entry.getKey().intValue())) {
				GoalEvent cachedEvent = getCachedGoalEvent(eventId);
				sendRescindedGoalMessage(cachedEvent);
				return true;
			}
			return false;
		});
	}
	
	void sendGoalMessage(GoalEvent event) {
		LOGGER.debug("Sending message for event [" + event + "].");
		String messageContent = CustomMessages.getCustomMessage(game.getScoringEvents(), event);
		if (messageContent != null) {
			sendMessage(messageContent);
		}
		MessageCreateSpec messageSpec = MessageCreateSpec.builder()
				.addEmbed(buildGoalMessageEmbed(this.game, event))
				.build();
		Message message = DiscordManager.sendAndGetMessage(channel, messageSpec);
		if (message != null) {
			goalEventMessages.put(event.getId(), message);
			meta.setGoalMessageIds(goalEventMessages);
			saveMetadata();
		}
	}
	
	void updateGoalMessage(GoalEvent event) {
		LOGGER.info("Updating message for event [" + event + "].");
		if (!goalEventMessages.containsKey(event.getId())) {
			LOGGER.warn("No message exists for the event: {}", event);
		} else {
			Message message = goalEventMessages.get(event.getId());
			MessageEditSpec messageSpec = MessageEditSpec.builder()
					.addEmbed(buildGoalMessageEmbed(this.game, event))
					.build();
			DiscordManager.updateMessage(message, messageSpec);
		}
	}

	void sendRescindedGoalMessage(GoalEvent event) {
		LOGGER.debug("Sending rescinded message for goal event [" + event + "].");
		if (event != null) {
			String player = game.getPlayer(event.getScorerId()).getFullName();
			sendMessage(String.format("Goal by %s has been rescinded.", player));
		}
	}

	private boolean isGoalEventUpdated(GoalEvent event) {
		GoalEvent cachedEvent = getCachedGoalEvent(event.getId());
		
		if(cachedEvent == null) {
			return false;
		}
		
		return cachedEvent.isUpdated(event);
	}

	public static EmbedCreateSpec buildGoalMessageEmbed(Game game, GoalEvent event) {
		EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();

		RosterPlayer scorer = game.getPlayer(event.getScorerId());

		if (game.getGameType().isShootout(event.getPeriod())) {
			return builder
			.color(scorer.getTeam().getColor())
			.addField(scorer.getFullName(), "Shootout goal", false)
			.footer("Shootout", null)
			.build();
		} else {
			String description = event.getTeam().getFullName() + " goal!";
			List<RosterPlayer> assistPlayers = event.getAssistIds().stream()
					.map(game::getPlayer)
					.collect(Collectors.toList());

			String assists = assistPlayers.size() > 0 
					? " Assists: " + assistPlayers.get(0).getFullName()
					: "(Unassisted)";
					
			if (assistPlayers.size() > 1) {
				assists += ", " + assistPlayers.get(1).getFullName();
			}
			String fAssists = assists;
			String time = game.getGameType().getPeriodCode(event.getPeriod()) + " @ " + event.getPeriodTime();
			return builder
					.description(description)
					.color(scorer.getTeam().getColor())
					.addField(scorer.getFullName(), fAssists, false)
					.footer(time, null)
					.build();
		}
	}

	/*
	 * Penalty Messages
	 */
	private PenaltyEvent getSkippablePenaltyEvent(int id) {
		return skippablePenaltyEvents.stream()
				.filter(skippableGoalEvent -> skippableGoalEvent.getId() == id)
				.findAny()
				.orElse(null);
	}
	
	private PenaltyEvent getCachedPenaltyEvent(int id) {
		return cachedPenaltyEvents.stream()
				.filter(cachedPenaltyEvent -> cachedPenaltyEvent.getId() == id)
				.findAny()
				.orElse(null);
	}

	/**
	 * <p>
	 * Sends/Updates messages for PenaltyEvents. If message has not been sent, it
	 * will be created. Otherwise, the existing message will be updated.
	 * </p>
	 * 
	 * <p>
	 * Does not update for events occurring before this thread (/the bot) was
	 * started.
	 * </p>
	 */
	private void updatePenaltyMessages(List<PenaltyEvent> penalties) {
		// Update Messages
		penalties.forEach(currentEvent -> {
			PenaltyEvent skippableEvent = getSkippablePenaltyEvent(currentEvent.getId());
			if (skippableEvent != null) {
				return;
			}
			if (!penaltyEventMessages.containsKey(currentEvent.getId())) {
				// New event
				sendPenaltyMessage(currentEvent);
			} else if (isPenaltyEventUpdated(currentEvent)) {
				// Updated event
				updatePenaltyMessage(currentEvent);
			}
		});

		// Remove Messages that do not have a corresponding event in the game's data.
		penaltyEventMessages.entrySet().removeIf(entry -> {
			int eventId = entry.getKey().intValue();
			if (penalties.stream().noneMatch(currentEvent -> currentEvent.getId() == entry.getKey().intValue())) {
				PenaltyEvent cachedEvent = getCachedPenaltyEvent(eventId);
				sendRescindedPenaltyMessage(cachedEvent);
				return true;
			}
			return false;
		});
	}
	
	private void sendPenaltyMessage(PenaltyEvent event) {
		LOGGER.debug("Sending message for event [" + event + "].");
		MessageCreateSpec messageSpec = MessageCreateSpec.builder()
				.addEmbed(buildPenaltyMessageEmbed(this.game, event))
				.build();
		Message message = DiscordManager.sendAndGetMessage(channel, messageSpec);
		if (message != null) {
			penaltyEventMessages.put(event.getId(), message);
			meta.setPenaltyMessageIds(penaltyEventMessages);
			saveMetadata();
		}
	}
	
	private void updatePenaltyMessage(PenaltyEvent event) {
		LOGGER.info("Updating message for event [" + event + "].");
		if (!penaltyEventMessages.containsKey(event.getId())) {
			LOGGER.warn("No message exists for the event: {}", event);
		} else {
			Message message = penaltyEventMessages.get(event.getId());

			MessageEditSpec messageSpec = MessageEditSpec.builder()
					.addEmbed(buildPenaltyMessageEmbed(this.game, event))
					.build();
			DiscordManager.updateMessage(message, messageSpec);
		}
	}

	private void sendRescindedPenaltyMessage(PenaltyEvent event) {
		LOGGER.debug("Sending rescinded message for penalty event [" + event + "].");
		sendMessage(String.format("Penalty has been rescinded. event=", event.toString()));
	}

	private boolean isPenaltyEventUpdated(PenaltyEvent event) {
		PenaltyEvent cachedEvent = getCachedPenaltyEvent(event.getId());
		
		if(cachedEvent == null) {
			return false;
		}
		
		return cachedEvent.isUpdated(event);
	}

	public static EmbedCreateSpec buildPenaltyMessageEmbed(Game game, PenaltyEvent event) {
		String header = String.format("%s - %s penalty", 
				event.getTeam().getLocation(), 
				event.getSeverity());
		StringBuilder description = new StringBuilder();
		
		RosterPlayer committedByPlayer = game.getPlayer(event.getCommittedByPlayerId());
		if (committedByPlayer != null) {
			description.append(committedByPlayer.getFullName());
		}
		
		description.append(String.format("\n**%s** - **%s** minutes",
				StringUtils.capitalize(event.getDescription()), event.getDuration()));
		
		String time = game.getGameType().getPeriodCode(event.getPeriod()) + " @ " + event.getPeriodTime();
		return EmbedCreateSpec.builder()
				.color(Color.BLACK)
				.addField(header, description.toString(), false)
				.footer(time, null)
				.build();
	}

	/*
	 * Summary Message
	 */
	private Message getSummaryMessage() {
		Message message = null;
		if (meta != null) {
			Long messageId = meta.getSummaryMessageId();
			if (messageId == null) {
				message = sendSummaryMessage();
			} else {
				message = nhlBot.getDiscordManager().getMessage(channel.getId().asLong(), messageId);
				if (message == null) {
					message = sendSummaryMessage();
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
		GDCScoreCommand.buildEmbed(embedBuilder, game);
		GDCGoalsCommand.buildEmbed(embedBuilder, game);
		return embedBuilder.build();
	}

	/*
	 * Voting Message
	 */
	private Message getVotingMessage() {
		Message message = null;
		if (meta != null) {
			Long messageId = meta.getVoteMessageId();
			if (messageId == null) {
				message = sendGDCHelpMessage(channel);
			} else {
				message = nhlBot.getDiscordManager().getMessage(channel.getId().asLong(), messageId);
				if (message == null) {
					message = sendGDCHelpMessage(channel);
				}
			}

			if (message != null) {
				DiscordManager.pinMessage(message);
				meta.setVoteMessageId(message.getId().asLong());
				saveMetadata();
			}
		}
		return message;
	}

	/*
	 * End of game message
	 */
	/**
	 * Updates/Sends the end of game message.
	 */
	void updateEndOfGameMessage() {
		if (endOfGameMessage == null) {
			if (channel != null) {
				endOfGameMessage = DiscordManager.sendAndGetMessage(channel, buildEndOfGameMessage());
			}
			if (endOfGameMessage != null) {
				LOGGER.debug("Sent end of game message for game. Pinning it...");
				DiscordManager.pinMessage(endOfGameMessage);
			}
		} else {
			LOGGER.trace("End of game message already sent.");
			String newEndOfGameMessage = buildEndOfGameMessage();
			Message updatedMessage = DiscordManager
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
		String message = "Game has ended. Thanks for joining!\n" + "Final Score: " + buildGameScore(game);

		List<Game> nextGames = preferences.getTeams().stream()
				.map(team -> nhlBot.getGameScheduler().getNextGame(team))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		if (!nextGames.isEmpty()) {
			if (nextGames.size() > 1) {

			} else {
				message += "\nThe next game is: "
						+ getGameDayChannel(nextGames.get(0)).getDetailsMessage();
			}
		}
		return message;
	}

	private static String buildGameScore(Game game) {
		return String.format("%s **%s** - **%s** %s", 
				game.getHomeTeam().getName(), game.getHomeScore(),
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

	/*
	 * Predictions
	 */
	@Override
	public void process(Event event) {
		// Do Nothing
	}
	/*
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
	
	private void removeReactions(Message message, Unicode excludedReaction, Snowflake userId) {
		for (Reaction messageReaction : message.getReactions()) {
			Unicode messageReactionUnicode = messageReaction.getEmoji().asUnicodeEmoji().orElse(null);
			if (messageReactionUnicode != null && !messageReactionUnicode.equals(excludedReaction)) {
				DiscordManager.subscribe(message.removeReaction(messageReactionUnicode, userId));
			}
		}
	}
	*/

	private Message sendDetailsMessage(TextChannel channel) {
		preferences.getTimeZone();
		String detailsMessage = getDetailsMessage();
		Message message = DiscordManager.sendAndGetMessage(channel, detailsMessage);
		DiscordManager.pinMessage(message);

		return message;
	}

	private Message sendGDCHelpMessage(TextChannel channel) {
		Message message = DiscordManager.sendAndGetMessage(channel,
				MessageCreateSpec.builder()
					.content(getHelpMessageText())
					.addEmbed(getVotingEmbedSpec())
					.build());
		DiscordManager.pinMessage(message);

		return message;
	}

	public void updateVotingMessage() {
		DiscordManager.updateMessage(votingMessage, 
				MessageEditSpec.builder()
					.contentOrNull(getHelpMessageText())
					.addEmbed(getVotingEmbedSpec())
					.build());
	}

	private String getHelpMessageText() {
		return String.format(
				"**This game/channel is interactable with Slash Commands!**"
				+ "\nUse `/gdc subcommand:help` to bring up a list of commands."
				+ "\n"
				+ "\n**Predict the outcome of this game!**"
				+ "\n%s: `/gdc subcommand:votehome`"
				+ "\n%s: `/gdc subcommand:voteaway`",
				game.getHomeTeam().getName(), 
				game.getAwayTeam().getName());
	}

	private EmbedCreateSpec getVotingEmbedSpec() {
		String campaignId = SeasonCampaign.buildCampaignId(Config.CURRENT_SEASON.getAbbreviation());
		List<Prediction> predictions = SeasonCampaign.loadPredictions(nhlBot, campaignId).stream()
				.filter(prediction -> prediction.getGamePk() == this.game.getGameId())
				.collect(Collectors.toList());
		long homeVotes = predictions.stream()
				.filter(prediction -> prediction.getPrediction() == this.game.getHomeTeam().getId())
				.count();
		long awayVotes = predictions.stream()
				.filter(prediction -> prediction.getPrediction() == this.game.getAwayTeam().getId())
				.count();
		Color color = Color.DISCORD_WHITE;
		if (homeVotes > awayVotes) {
			color = game.getHomeTeam().getColor();
		} else if (homeVotes < awayVotes) {
			color = game.getAwayTeam().getColor();
		}
		EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();
		embedBuilder
			.title("Current Votes")
			.color(color)
			.addField(
				game.getHomeTeam().getName(),
				String.valueOf(homeVotes),
				true
			)
			.addField(
				"vs",
				"~~", // For formatting
				true
			)
			.addField(
				game.getAwayTeam().getName(),
				String.valueOf(awayVotes),
				true
			);
		return embedBuilder.build();
	}

	private void sendWordcloud() {
		new WordcloudCommand(nhlBot).sendWordcloud(channel, game);
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
	public static String getNiceDate(Game game, ZoneId zone) {
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
		return getNiceDate(game, zone);
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
	 *            the game to get the message for
	 * @param timeZone
	 *            the time zone to localize to
	 * 
	 * @return message in the format: "The next game is:\n<br>
	 *         **Home Team** vs **Away Team** at HH:mm aaa on EEEE dd MMM yyyy"
	 */
	public static String getDetailsMessage(Game game) {
		String message = String.format("**%s** vs **%s** at <t:%s>", 
				game.getHomeTeam().getFullName(),
				game.getAwayTeam().getFullName(), 
				game.getStartTime().toEpochSecond());
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
	public String getDetailsMessage() {
		return getDetailsMessage(game);
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
	 * Stops the thread and deletes the channel from the Discord Guild.
	 */
	void stopAndRemoveGuildChannel() {
		DiscordManager.deleteChannel(channel);
		interrupt();
	}

	@Override
	public void interrupt() {
		// unregisterFromListener(); Deregister reaction listener
		super.interrupt();
	}
}
