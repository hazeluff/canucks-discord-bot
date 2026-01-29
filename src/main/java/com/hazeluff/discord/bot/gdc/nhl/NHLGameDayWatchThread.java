package com.hazeluff.discord.bot.gdc.nhl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMeta;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.nhl.NHLGameTracker;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;

public class NHLGameDayWatchThread extends NHLGameDayThread {
	private static final Logger LOGGER = LoggerFactory.getLogger(NHLGameDayWatchThread.class);

	@Override
	protected Logger LOGGER() {
		return LOGGER;
	}

	private NHLGameDayWatchThread(NHLBot nhlBot, NHLGameTracker gameTracker, Guild guild, TextChannel channel,
			GuildPreferences preferences, GDCMeta meta) {
		super(nhlBot, gameTracker, guild, channel, preferences, meta);
	}

	public static NHLGameDayWatchThread get(NHLBot nhlBot, TextChannel textChannel, NHLGameTracker gameTracker, Guild guild) {
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
		NHLGameDayWatchThread gdt = new NHLGameDayWatchThread(nhlBot, gameTracker, guild,
				textChannel, preferences, meta);
		if (gdt.channel != null) {
			gdt.start();
		} else {
			LOGGER.warn("GameDayChannel not started. TextChannel could not be found. guild={}", guild.getId().asLong());
		}
		return gdt;
	}

	@Override
	protected void initChannel() {
		loadMetadata();
		initIntroMessage();
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

	/*
	 * Run method overrides
	 */
	@Override
	protected void setThreadName() {
		setName("G:" + StringUtils.abbreviate(guild.getId().asString(), 9) + game.getGameId());
		// Game Id should be 10 digits
	}

	@Override
	protected String buildStartOfGameMessage() {
		String message = String.format("%s\n", getMatchupName());
		message += "Game is about to start!";
		return message;
	}
}
