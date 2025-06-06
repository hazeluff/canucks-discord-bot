package com.hazeluff.discord.bot.command.gdc;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.Command;
import com.hazeluff.discord.bot.gdc.nhl.NHLGameDayChannelThread;
import com.hazeluff.nhl.game.Game;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

public class GDCSyncCommand extends GDCSubCommand {

	@Override
	public String getName() {
		return "sync";
	}

	@Override
	public String getDescription() {
		return "Sync the game's data.";
	}

	@Override
	public Publisher<?> reply(ChatInputInteractionEvent event, NHLBot nhlBot, Game game) {
		NHLGameDayChannelThread gameDayChannel = getGameDayChannel(event, nhlBot, game);
		gameDayChannel.refresh();
		return Command.reply(event, "Game synced.");
	}

}
