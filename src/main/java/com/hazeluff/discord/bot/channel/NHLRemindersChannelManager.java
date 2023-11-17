package com.hazeluff.discord.bot.channel;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.utils.DiscordGuildEnitityManager;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;

public class NHLRemindersChannelManager extends DiscordGuildEnitityManager<TextChannel> {

	public static final String CHANNEL_NAME = "nhl-reminders";

	public NHLRemindersChannelManager(NHLBot nhlBot) {
		super(nhlBot);
	}

	@Override
	public TextChannel fetch(Guild guild) {
		return DiscordManager.getOrCreateTextChannel(guild, CHANNEL_NAME);
	}
}
