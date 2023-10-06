package com.hazeluff.discord.bot.command.stats;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.nhl.stats.SkaterStats;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

public class NHLDefenderStatsCommand extends NHLSkaterStatsCommand {

	@Override
	public String getName() {
		return "defenders";
	}

	@Override
	public String getDescription() {
		return "Stats of Defenders.";
	}

	@Override
	public Publisher<?> reply(ChatInputInteractionEvent event, NHLBot nhlBot) {
		return reply(event, nhlBot, SkaterStats::isDefender, "Defender");
	}
}
