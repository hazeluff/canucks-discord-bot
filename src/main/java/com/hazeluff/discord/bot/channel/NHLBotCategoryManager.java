package com.hazeluff.discord.bot.channel;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.utils.DiscordGuildEnitityManager;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.Category;

public class NHLBotCategoryManager extends DiscordGuildEnitityManager<Category> {

	public static final String CATEGORY_NAME = "NHLBot";

	public NHLBotCategoryManager(NHLBot nhlBot) {
		super(nhlBot);
	}

	@Override
	public Category fetch(Guild guild) {
		return DiscordManager.getOrCreateCategory(guild, CATEGORY_NAME);
	}
}
