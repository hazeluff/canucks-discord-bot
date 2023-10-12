package com.hazeluff.discord.bot.command.stats;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.nhl.stats.SkaterStats;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

public class NHLForwardStatsCommand extends NHLSkaterStatsCommand {

	@Override
	public String getName() {
		return "forwards";
	}

	@Override
	public String getDescription() {
		return "Stats of Forwards.";
	}

	@Override
	public Publisher<?> reply(ChatInputInteractionEvent event, NHLBot nhlBot) {
		return reply(event, nhlBot, 
				"Forward",
				SkaterStats::isForward,
				(SkaterStats s1, SkaterStats s2) -> s2.getPoints() - s1.getPoints()
		);
	}
}
