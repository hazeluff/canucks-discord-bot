package com.hazeluff.discord.bot.channel;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.utils.DiscordGuildEnitityManager;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;

public class WordcloudChannelManager extends DiscordGuildEnitityManager<TextChannel> {

	private static final String CHANNEL_NAME = "wordcloud";

	private final GDCCategoryManager gdcCategoryManager;

	public WordcloudChannelManager(NHLBot nhlBot, GDCCategoryManager gdcCategoryManager) {
		super(nhlBot);
		this.gdcCategoryManager = gdcCategoryManager;
	}

	@Override
	public TextChannel fetch(Guild guild) {
		TextChannel channel = DiscordManager.getOrCreateTextChannel(guild, CHANNEL_NAME);
		if (channel != null) {
			Category gdcCategory = gdcCategoryManager.get(guild);
			if (gdcCategory != null && !channel.getCategoryId().isPresent()) {
				DiscordManager.moveChannel(gdcCategory, channel);
			}
		}
		return channel;
	}
}
