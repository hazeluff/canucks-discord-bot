package com.hazeluff.discord.bot.gdc.nhl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.hazeluff.discord.bot.NHLBot;

import discord4j.core.object.entity.Guild;

public class NHLGameDayChannelsManager {

	private final NHLBot nhlBot;

	private final Map<Long, NHLGameDayWatchChannel> channels = new ConcurrentHashMap<>();

	public NHLGameDayChannelsManager(NHLBot nhlBot) {
		this.nhlBot = nhlBot;
	}
	
	public void init() {
		nhlBot.getDiscordManager().getClient()
			.getGuilds()
				.subscribe(guild -> createChannel(guild));
	}

	private NHLGameDayWatchChannel createChannel(Guild guild) {
		NHLGameDayWatchChannel channel = NHLGameDayWatchChannel.createChannel(nhlBot, guild);
		channels.put(guild.getId().asLong(), channel);
		return channel;
	}

	public NHLGameDayWatchChannel getChannel(Guild guild) {
		return channels.get(guild.getId().asLong());
	}
}
