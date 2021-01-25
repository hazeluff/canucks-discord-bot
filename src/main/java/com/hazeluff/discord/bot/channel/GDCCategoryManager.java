package com.hazeluff.discord.bot.channel;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.utils.DiscordGuildEnitityManager;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.Category;

public class GDCCategoryManager extends DiscordGuildEnitityManager<Category> {

	public static final String CATEGORY_NAME = "Game Day Channels";

	public GDCCategoryManager(NHLBot nhlBot) {
		super(nhlBot);
	}

	@Override
	public Category fetch(Guild guild) {
		return nhlBot.getDiscordManager().getOrCreateCategory(guild, CATEGORY_NAME);
	}
}
