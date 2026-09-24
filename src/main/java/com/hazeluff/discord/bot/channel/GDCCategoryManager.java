package com.hazeluff.discord.bot.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.utils.DiscordGuildEnitityManager;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.Category;

public class GDCCategoryManager extends DiscordGuildEnitityManager<Category> {
	private static final Logger LOGGER = LoggerFactory.getLogger(GDCCategoryManager.class);

	public static final String CATEGORY_NAME = "Game Day Channels";

	public GDCCategoryManager(NHLBot nhlBot) {
		super(nhlBot);
	}

	@Override
	public Category fetch(Guild guild) {
		long guildId = guild.getId().asLong();
		GuildPreferences pref = nhlBot.getPersistentData().getPreferencesData().getGuildPreferences(guildId);
		Category category = null;
		try {
			// Attempt to fetch channel by the saved preferences
			Long prefCategoryId = pref.getGDCCategoryId();
			if (prefCategoryId != null) {
				nhlBot.getDiscordManager();
				category = DiscordManager.getCategory(guild, prefCategoryId);
			}
			if (category == null) {
				category = DiscordManager.getCategory(guild, CATEGORY_NAME);
			}
		} catch (Exception e) {
			LOGGER.warn("Problem fetching existing category.");
		}
		// Failures return null; null categories mean channels are dumped at the root
		return category;
	}
}
