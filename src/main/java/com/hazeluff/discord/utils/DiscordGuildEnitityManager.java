package com.hazeluff.discord.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hazeluff.discord.bot.NHLBot;

import discord4j.core.object.entity.Guild;

public abstract class DiscordGuildEnitityManager<T> {
	protected final NHLBot nhlBot;

	protected final Map<Long, T> entities = new HashMap<>();

	public DiscordGuildEnitityManager(NHLBot nhlBot) {
		this.nhlBot = nhlBot;
	}

	public void init(List<Guild> guilds) {
		for (Guild guild : guilds) {
			get(guild);
		}
	}

	public T get(Guild guild) {
		long guildId = guild.getId().asLong();
		T entity = entities.get(guildId);
		if (entity == null) {
			entity = fetch(guild);
			if (entity != null) {
				entities.put(guildId, entity);
			}
		}
		return entity;
	}

	protected abstract T fetch(Guild guild);
}
