package com.hazeluff.discord.bot.gdc.ahl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.ahl.game.Game;
import com.hazeluff.ahl.game.event.Player;
import com.hazeluff.ahl.game.event.ShootoutEvent;
import com.hazeluff.discord.bot.NHLBot;
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
public class ShootoutMessagesManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(ShootoutMessagesManager.class);
	@SuppressWarnings("unused")
	private final NHLBot nhlBot;
	@SuppressWarnings("unused")
	private final Game game;
	private final TextChannel channel;

	private List<ShootoutEvent> prerunEvents = new ArrayList<>(); // Events before the start of the thread
	private List<ShootoutEvent> cachedEvents = new ArrayList<>(); // Last known state of events
	private final List<Message> eventMessages = new ArrayList<>();

	public ShootoutMessagesManager(NHLBot nhlBot, Game game, TextChannel channel) {
		this.nhlBot = nhlBot;
		this.game = game;
		this.channel = channel;
	}

	public void initEvents(List<ShootoutEvent> events) {
		this.prerunEvents = events;
		this.cachedEvents = events;
	}

	public void updateCache(List<ShootoutEvent> events) {
		this.cachedEvents = events;
	}

	private boolean isEventUpdated(int index, ShootoutEvent event) {
		if (index >= cachedEvents.size()) {
			return false;
		}
		return cachedEvents.get(index).isUpdated(event);
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
	public void updateMessages(List<ShootoutEvent> shootoutEvents) {
		// Update Messages
		for (int eventIdx = 0; eventIdx < shootoutEvents.size(); eventIdx++) {
			if (eventIdx >= prerunEvents.size()) {
				continue;
			}

			if (isEventUpdated(eventIdx, shootoutEvents.get(eventIdx))) {
				// Updated event
				updateMessage(eventIdx, shootoutEvents.get(eventIdx));
				return;
			}

			if (eventIdx < eventMessages.size()) {
				sendMessage(shootoutEvents.get(eventIdx));
			}
		}

		// Remove Messages that do not have a corresponding event in the game's data.
		while (eventMessages.size() > cachedEvents.size()) {
			Message messageToDelete = eventMessages.remove(eventMessages.size() - 1);
			DiscordManager.deleteMessage(messageToDelete);
		}

		updateCache(shootoutEvents);
	}

	void sendMessage(ShootoutEvent event) {
		LOGGER.info("Sending message for event [{}].", event.getShooter().getId());
		MessageCreateSpec messageSpec = MessageCreateSpec.builder()
				.addEmbed(buildShootoutMessageEmbed(event))
				.build();
		Message message = DiscordManager.sendAndGetMessage(channel, messageSpec);
		if (message != null) {
			eventMessages.add(message);
		}
	}

	void updateMessage(int index, ShootoutEvent event) {
		LOGGER.info("Updating message for event [{}] at index [{}].", event.getShooter().getId(), index);
		if (index >= eventMessages.size()) {
			LOGGER.warn("No message exists for the event: {}", event);
		} else {
			Message message = eventMessages.get(index);
			if (message != null) {
				MessageEditSpec messageSpec = MessageEditSpec.builder()
						.addEmbed(buildShootoutMessageEmbed(event))
						.build();
				DiscordManager.updateMessage(message, messageSpec);
			}
		}
	}

	public static EmbedCreateSpec buildShootoutMessageEmbed(ShootoutEvent event) {
		EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
		Player shooter = event.getShooter();
		Color embedColor = event.isGoal()
				? event.getTeam().getColor()
				: Color.WHITE;

		String description = event.getTeam().getLocationName() + "shootout attempt:";
		description += "\n" + shooter.getFullName();
		if (event.isGoal()) {
			description += "\n**Goal**";
		} else {
			description += "\n**Miss**";
		}

		return builder.description(description)
				.color(embedColor)
				.footer("Shootout", null).build();
	}
	
	protected void sendMessage(String message) {
		if (channel != null) {
			DiscordManager.sendMessage(channel, message);
		}
	}
}
