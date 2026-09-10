package com.hazeluff.discord.bot.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.utils.DiscordGuildEnitityManager;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;

public class WordcloudChannelManager extends DiscordGuildEnitityManager<TextChannel> {
	private static final Logger LOGGER = LoggerFactory.getLogger(WordcloudChannelManager.class);

	private static final String CHANNEL_NAME = "wordcloud";

	public WordcloudChannelManager(NHLBot nhlBot) {
		super(nhlBot);
	}

	@Override
	public TextChannel fetch(Guild guild) {
		long guildId = guild.getId().asLong();
		GuildPreferences pref = nhlBot.getPersistentData().getPreferencesData().getGuildPreferences(guildId);
		TextChannel channel = null;
		try {
			// Attempt to fetch channel by the saved preferences
			Long prefChannelId = pref.getWordcloudChannelId();
			if (prefChannelId != null) {
				nhlBot.getDiscordManager();
				channel = DiscordManager.getTextChannel(guild, prefChannelId);
			}
			if (channel == null) {
				channel = DiscordManager.getTextChannels(guild).stream()
					.filter(guildChannel -> guildChannel.getName().equals(CHANNEL_NAME)).findFirst().orElse(null);
			}
		} catch (Exception e) {
			LOGGER.warn("Problem fetching existing channel.");
		}
		return channel;
	}
}
