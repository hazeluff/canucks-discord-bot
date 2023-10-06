package com.hazeluff.discord.bot.command.stats;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.nhl.Seasons.Season;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

public abstract class NHLStatsSubCommand {
	public abstract String getName();

	public abstract String getDescription();

	public abstract Publisher<?> reply(ChatInputInteractionEvent event, NHLBot nhlBot);

	/**
	 * Gets a season given a season code or value. e.g. 99 -> 1999, 20 -> 2020
	 * 
	 * @param startYear
	 * @return
	 */
	static Season getSeason(Long startYear) {
		if (startYear != null) {
			int year = startYear.intValue();
			if (year >= 0 && year < (Config.CURRENT_SEASON.getStartYear() - 2000)) {
				year = year + 2000;
			} else if (year >= (Config.CURRENT_SEASON.getStartYear() - 2000) && year < 100) {
				year = year + 1900;
			}
			return Season.mock(year);
		}
		return Config.CURRENT_SEASON;
	}
}
