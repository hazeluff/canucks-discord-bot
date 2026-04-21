package com.hazeluff.discord.bot.gdc.nhl;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMeta;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.bot.gdc.GameDayThread;
import com.hazeluff.discord.nhl.NHLGameTracker;
import com.hazeluff.nhl.game.Game;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.ThreadChannel;
import discord4j.discordjson.json.StartThreadFromMessageRequest;

public class NHLGameDayWatchThread extends NHLGameDayThread {
	private static final Logger LOGGER = LoggerFactory.getLogger(NHLGameDayWatchThread.class);

	private final boolean isChannelThread;

	@Override
	protected Logger LOGGER() {
		return LOGGER;
	}

	private NHLGameDayWatchThread(NHLBot nhlBot, NHLGameTracker gameTracker, Guild guild, MessageChannel channel,
		GDCMeta meta, boolean isThread
	) {
		super(nhlBot, gameTracker, guild, channel, meta, false);
		this.isChannelThread = isThread;
	}

	public static NHLGameDayWatchThread getOrCreate(NHLBot nhlBot, MessageChannel messageChannel,
		NHLGameTracker gameTracker, Guild guild, boolean useThreads
	) {
		long guildId = guild.getId().asLong();
		int gameId = gameTracker.getGame().getGameId();

		GDCMeta meta = null;
		if (messageChannel == null) {
			LOGGER.warn(
				"messageChannel is null (no parent for Thread/no Channel). GameDayChannel not started. guild={}",
				guildId);
			return new NHLGameDayWatchThread(nhlBot, gameTracker, guild, messageChannel, meta, useThreads);
		}

		MessageChannel parentChannel = messageChannel;
		long parentChannelId = parentChannel.getId().asLong();

		// GDC in threads
		if (useThreads) {
			messageChannel = null;
			meta = nhlBot.getPersistentData().getGDCMetaData().loadMetaByParentId(parentChannelId, gameId);

			// Try to get existing Thread (Channel)
			if (meta != null) {
				messageChannel = DiscordManager.getMessageChannel(guild, meta.getChannelId());
			}

			// Create new Thread (Channel) if none found/exist.
			if (messageChannel == null) {
				meta = null; // Generate new meta
				Game game = gameTracker.getGame();
				String threadMsg = "Game Day Thread: " + GameDayThread.buildDetailsMessage(game);
				Message message = DiscordManager.sendAndGetMessage(parentChannel, threadMsg);
				if (message != null) {
					StartThreadFromMessageRequest request = StartThreadFromMessageRequest.builder()
						.name(game.getThreadName()).build();
					ThreadChannel threadChannel = DiscordManager.block(message.createPublicThread(request));
					if (threadChannel != null) {
						messageChannel = threadChannel;
					}
				}
			}
		} else {
			meta = nhlBot.getPersistentData().getGDCMetaData().loadMetaByChannelId(parentChannelId, gameId);
			// WatchThread will be made from messageChannel
		}

		// Channel exists; No meta exists.
		if (messageChannel != null && meta == null) {
			// Generate new metadata
			if (useThreads)
				meta = GDCMeta.forThread(messageChannel.getId().asLong(), gameId, parentChannelId);
			else
				meta = GDCMeta.forChannel(messageChannel.getId().asLong(), gameId);
			nhlBot.getPersistentData().getGDCMetaData().save(meta);
		}

		// Make and return WatchThread
		NHLGameDayWatchThread gdt = new NHLGameDayWatchThread(nhlBot, gameTracker, guild, messageChannel, meta,
			useThreads);
		if (gdt.channel != null) {
			gdt.start();
		} else {
			LOGGER.warn("GameDayChannel not started. `messageChannel` was null. guild={}", guildId);
		}
		return gdt;
	}

	@Override
	protected void initChannel() {
		loadMetadata();
		initIntroMessage();
		saveMetadata();
	}

	@Override
	protected void updateActive() {
		updateMessages();
	}

	@Override
	protected void updateEnd() {
		sendEndOfGameMessage();
		Message summaryMessage = sendSummaryMessage(); // Avoid save/load of metadata
		DiscordManager.pinMessage(summaryMessage);
	}

	@Override
	protected String buildReminderMessage(String basicMessage) {
		return getMatchupName() + ": " + basicMessage;
	}

	@Override
	protected Map<Long, String> getReminders() {
		return new HashMap<Long, String>() {
			{
				put(3600000l, "60 minutes till puck drop.");
			}
		};
	}

	@Override
	protected String buildStartOfGameMessage() {
		String message = "Game is about to start!";
		if (isChannelThread)
			return message;
		else
			return getMatchupName() + "\n" + message;
	}
}
