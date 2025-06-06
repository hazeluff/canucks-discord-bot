package com.hazeluff.discord.bot.gdc.ahl;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.ahl.game.Game;
import com.hazeluff.ahl.game.event.GoalEvent;
import com.hazeluff.ahl.game.event.Player;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMeta;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.utils.DateUtils;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.rest.util.Color;

/**
 * Used by {@link AHLGameDayChannel} to manage spamming and orphaned event +
 * messages.
 */
public class GoalMessagesManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(GoalMessagesManager.class);
	private static final long COOLDOWN = 30000l;
	private final NHLBot nhlBot;
	private final Game game;
	private final TextChannel channel;
	private final GDCMeta meta;

	private List<GoalEvent> prerunEvents = new ArrayList<>(); // Events before the start of the thread
	private List<GoalEvent> cachedEvents = new ArrayList<>(); // Last known state of events
	private final Map<Integer, Pair<GoalEvent, Message>> eventMessages = new HashMap<>();

	private ZonedDateTime lastMessageTime;

	public GoalMessagesManager(NHLBot nhlBot, Game game, TextChannel channel, GDCMeta meta) {
		this.nhlBot = nhlBot;
		this.game = game;
		this.channel = channel;
		this.meta = meta;
	}

	public void initEvents(List<GoalEvent> events) {
		this.prerunEvents = events;
		this.cachedEvents = events;
	}

	public void updateCache(List<GoalEvent> events) {
		this.cachedEvents = events;
	}

	public GoalEvent getCachedEvent(int id) {
		return cachedEvents.stream()
				.filter(cachedGoalEvent -> cachedGoalEvent.getGoalId() == id)
				.findAny()
				.orElse(null);
	}

	private boolean isEventUpdated(GoalEvent event) {
		GoalEvent cachedEvent = getCachedEvent(event.getGoalId());
		if(cachedEvent == null) {
			return false;
		}
		return cachedEvent.isUpdated(event);
	}

	public boolean isPrerunEvent(GoalEvent goalEvent) {
		return prerunEvents.stream().anyMatch(skippableGoalEvent -> 
				skippableGoalEvent.getGoalId() == goalEvent.getGoalId());
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
	public void updateMessages(List<GoalEvent> goals) {
		// Update Messages
		goals.forEach(currentEvent -> {
			if (isPrerunEvent(currentEvent)) {
				// Event happened before game start
				return;
			}
			if (isEventUpdated(currentEvent)) {
				// Updated event
				updateMessage(currentEvent);
				return;
			}
			if (!eventMessages.containsKey(currentEvent.getGoalId())) {
				// Try to relink event that may be reposted
				boolean isRelinked = relinkEventMessage(currentEvent);
				if (isRelinked) {
					updateMessage(currentEvent);
					return;
				}

				// New event
				sendMessage(currentEvent);
			}
		});

		// Remove Messages that do not have a corresponding event in the game's data.
		eventMessages.entrySet().removeIf(
				entry -> goals.stream().noneMatch(
						currentEvent -> currentEvent.getGoalId() == entry.getKey().intValue()));

		updateCache(goals);
	}

	public void initEventMessages(Map<Integer, Long> messages) {
		for (Entry<Integer, Long> entry : messages.entrySet()) {
			Integer eventId = entry.getKey();
			Long messageId = entry.getValue();
			Message message = nhlBot.getDiscordManager().getMessage(channel.getId().asLong(), messageId);
			if (message != null) {
				this.eventMessages.put(eventId, Pair.with(null, message));
			} else {
				LOGGER.warn("Failed to find message: " + messageId);
			}
		}
	}

	void sendMessage(GoalEvent event) {
		LOGGER.info("Sending message for event [" + event.getGoalId() + "].");
		/*
		if (isSpam()) {
			LOGGER.warn("Spam message avoided. eventId={}", event.getGoalId());
			eventMessages.put(event.getGoalId(), Pair.with(event, null)); // Pretend to send the message
			return;
		}
		*/

		MessageCreateSpec messageSpec = MessageCreateSpec.builder()
				.addEmbed(buildGoalMessageEmbed(this.game, event))
				.build();
		Message message = DiscordManager.sendAndGetMessage(channel, messageSpec);
		if (message != null) {
			eventMessages.put(event.getGoalId(), Pair.with(event, message));
			setMetaGoalMessageIds(eventMessages);
			saveMetadata();
			updateLastMessageTime();
		}
	}

	private void setMetaGoalMessageIds(Map<Integer, Pair<GoalEvent, Message>> goalMessages) {
		Map<Integer, Long> message = goalMessages.entrySet().stream()
				.collect(Collectors.toMap(
						e -> e.getKey(),
						e -> e.getValue().getValue1().getId().asLong())
				);
		meta.setGoalMessageIds(message);
	}

	void updateMessage(GoalEvent event) {
		LOGGER.info("Updating message for event [" + event.getGoalId() + "].");
		if (!eventMessages.containsKey(event.getGoalId())) {
			LOGGER.warn("No message exists for the event: {}", event);
		} else {
			Message message = eventMessages.get(event.getGoalId()).getValue1();
			if (message != null) {
				MessageEditSpec messageSpec = MessageEditSpec.builder()
						.addEmbed(buildGoalMessageEmbed(this.game, event))
						.build();
				DiscordManager.updateMessage(message, messageSpec);
				eventMessages.put(event.getGoalId(), Pair.with(event, message));
			}
		}
	}

	boolean relinkEventMessage(GoalEvent event) {
		Pair<GoalEvent, Message> eventMessage = eventMessages.entrySet().stream()
			.map(entry -> entry.getValue())
			.filter(em -> em.getValue1() != null)
			.filter(em -> em.getValue0() != null && em.getValue0().isSameTime(event))
			.findAny()
			.orElse(null);

		// No existing message
		if (eventMessage == null) {
			return false;
		}

		// Message's event has a different time signature
		if (eventMessage.getValue0().isSameTime(event)) {
			return false;
		}

		LOGGER.info("Relink message to new event [" + event.getGoalId() + "].");
		Message message = eventMessage.getValue1();
		eventMessages.put(event.getGoalId(), Pair.with(event, message));

		return true;
	}

	public static EmbedCreateSpec buildGoalMessageEmbed(Game game, GoalEvent event) {
		EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
		Player scorer = event.getGoalDetails().getScorer();
		Color embedColor = scorer != null
				? event.getGoalDetails().getTeam().getColor()
				: Color.WHITE;

		String description = event.getGoalDetails().getTeam().getLocationName();
		if (event.getGoalDetails().isEmptyNet()) {
			description += " empty-net";
		}
		if (event.getGoalDetails().isPenaltyShot()) {
			description += " penalty-shot";
		}
		if (event.getGoalDetails().isPowerPlay()) {
			description += " power-play";
		}
		if (event.getGoalDetails().isShortHanded()) {
			description += " short-handed";
		}

		description += " goal!";
		List<Player> assistPlayers = event.getGoalDetails().getAssists();

		String assists = assistPlayers.size() > 0
				? " Assists: " + assistPlayers.get(0).getFullName()
				: "(Unassisted)";
		if (assistPlayers.size() > 1) {
			assists += ", " + assistPlayers.get(1).getFullName();
		}
		String fAssists = assists;
		String time = event.getGoalDetails().getPeriodLongName() + " @ " + event.getGoalDetails().getTime();
		return builder.description(description)
				.color(embedColor)
				.addField(scorer.getFullName(), fAssists, false)
				.footer(time, null).build();
	}

	public void updateLastMessageTime() {
		this.lastMessageTime = ZonedDateTime.now();
	}

	public boolean isSpam() {
		if (lastMessageTime == null) {
			return false;
		}
		return DateUtils.diffMs(lastMessageTime, DateUtils.now()) < COOLDOWN;
	}
	
	protected void sendMessage(String message) {
		if (channel != null) {
			DiscordManager.sendMessage(channel, message);
		}
	}

	private void saveMetadata() {
		nhlBot.getPersistentData().getGDCMetaData().save(meta);
	}
}
