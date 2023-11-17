package com.hazeluff.discord.bot.channel;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.utils.DiscordGuildEnitityManager;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;

public class NHLReminderChannelManager extends DiscordGuildEnitityManager<TextChannel> {

	public static final String CATEGORY_NAME = "Game Day Channels";

	public NHLReminderChannelManager(NHLBot nhlBot) {
		super(nhlBot);
	}

	@Override
	public TextChannel fetch(Guild guild) {
		return DiscordManager.getOrCreateTextChannel(guild, "nhl-reminders");
	}
}
