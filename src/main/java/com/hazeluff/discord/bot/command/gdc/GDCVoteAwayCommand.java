package com.hazeluff.discord.bot.command.gdc;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.nhl.game.Game;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

public class GDCVoteAwayCommand extends GDCVoteCommand {

	@Override
	public String getName() {
		return "voteaway";
	}

	@Override
	public String getDescription() {
		return "Vote for the Away team to win!";
	}

	@Override
	public Publisher<?> reply(ChatInputInteractionEvent event, NHLBot nhlBot, Game game) {
		return replyVoteTeam(event, nhlBot, game, game.getAwayTeam());
	}
}
