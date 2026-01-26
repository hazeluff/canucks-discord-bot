package com.hazeluff.discord.bot.gdc.ahl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.ahl.game.Game;
import com.hazeluff.ahl.game.event.PenaltyEvent;
import com.hazeluff.ahl.game.event.Player;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMeta;
import com.hazeluff.discord.bot.discord.DiscordManager;

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
public class PenaltyMessagesManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(PenaltyMessagesManager.class);
	private final NHLBot nhlBot;
	private final Game game;
	private final TextChannel channel;
	private final GDCMeta meta;

	private List<PenaltyEvent> prerunEvents = new ArrayList<>(); // Events before the start of the thread
	private List<PenaltyEvent> cachedEvents = new ArrayList<>(); // Last known state of events
	private final Map<Integer, Message> eventMessages = new HashMap<>();

	public PenaltyMessagesManager(NHLBot nhlBot, Game game, TextChannel channel, GDCMeta meta) {
		this.nhlBot = nhlBot;
		this.game = game;
		this.channel = channel;
		this.meta = meta;
	}

	public void initEvents(List<PenaltyEvent> events) {
		this.prerunEvents = events;
		this.cachedEvents = events;
	}

	public void updateCache(List<PenaltyEvent> events) {
		this.cachedEvents = events;
	}

	public PenaltyEvent getCachedEvent(int id) {
		return cachedEvents
				.stream()
				.filter(cachedPenaltyEvent -> cachedPenaltyEvent.getPenaltyId() == id)
				.findAny()
				.orElse(null);
	}

	private boolean isEventUpdated(PenaltyEvent event) {
		PenaltyEvent cachedEvent = getCachedEvent(event.getPenaltyId());
		if(cachedEvent == null) {
			return false;
		}
		return cachedEvent.isUpdated(event);
	}

	public boolean isPrerunEvent(PenaltyEvent PenaltyEvent) {
		return prerunEvents
				.stream()
				.anyMatch(skippablePenaltyEvent -> skippablePenaltyEvent.getPenaltyId() == PenaltyEvent.getPenaltyId());
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
	public void updateMessages(List<PenaltyEvent> penalties) {
		// Update Messages
		penalties.forEach(currentEvent -> {
			if (isPrerunEvent(currentEvent)) {
				// Event happened before game start
				return;
			}
			if (isEventUpdated(currentEvent)) {
				// Updated event
				updateMessage(currentEvent);
			}
			if (!eventMessages.containsKey(currentEvent.getPenaltyId())) {
				// New event
				sendMessage(currentEvent);
			}
		});

		// Remove Messages that do not have a corresponding event in the game's data.
		eventMessages.entrySet()
				.removeIf(
						entry -> penalties.stream()
							.noneMatch(currentEvent -> currentEvent.getPenaltyId() == entry.getKey().intValue()));

		updateCache(penalties);
	}

	public void initEventMessages(Map<Integer, Long> messages) {
		for (Entry<Integer, Long> entry : messages.entrySet()) {
			Integer eventId = entry.getKey();
			Long messageId = entry.getValue();
			Message message = nhlBot.getDiscordManager().getMessage(channel.getId().asLong(), messageId);
			if (message != null) {
				this.eventMessages.put(eventId, message);
			} else {
				LOGGER.warn("Failed to find message: " + messageId);
			}
		}
	}

	void sendMessage(PenaltyEvent event) {
		LOGGER.debug("Sending message for event [" + event.getPenaltyId() + "].");
		MessageCreateSpec messageSpec = MessageCreateSpec.builder()
				.addEmbed(buildPenaltyMessageEmbed(this.game, event))
				.build();
		Message message = DiscordManager.sendAndGetMessage(channel, messageSpec);
		if (message != null) {
			eventMessages.put(event.getPenaltyId(), message);
			meta.setPenaltyMessageIds(eventMessages);
			saveMetadata();
		}
	}

	void updateMessage(PenaltyEvent event) {
		LOGGER.info("Updating message for event [" + event.getPenaltyId() + "].");
		if (!eventMessages.containsKey(event.getPenaltyId())) {
			LOGGER.warn("No message exists for the event: {}", event);
		} else {
			Message message = eventMessages.get(event.getPenaltyId());

			MessageEditSpec messageSpec = MessageEditSpec.builder()
					.addEmbed(buildPenaltyMessageEmbed(this.game, event))
					.build();
			DiscordManager.updateMessage(message, messageSpec);
		}
	}

	public static EmbedCreateSpec buildPenaltyMessageEmbed(Game game, PenaltyEvent event) {
		String header = event.getTeam().getLocationName() + " -";
		if(event.isBenchPenalty()) {
			header += " bench";
		}
		header += " penalty";

		StringBuilder description = new StringBuilder();
		Player takenByPlayer = event.getTakenBy();
		if (takenByPlayer != null) {
			description.append(takenByPlayer.getFullName() + " (" + takenByPlayer.getJerseyNumber() + ")");
		}
		description.append(String.format("\n**%s** - **%s** minutes",
				StringUtils.capitalize(event.getDescription()), event.getDuration()));
		Player servedByPlayer = event.getServedBy();
		if (servedByPlayer != null) {
			description.append("\nServed by: " + servedByPlayer.getFullName() + " (" + takenByPlayer.getJerseyNumber() + ")");
		}

		String time = event.getPeriodLongName() + " @ " + event.getTime();

		EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
		return builder
				.color(Color.BLACK)
				.addField(header, description.toString(), false)
				.footer(time, null)
				.build();
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
