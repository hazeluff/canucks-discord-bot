package com.hazeluff.discord.bot.command.gdc;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.GDCCommand;
import com.hazeluff.discord.bot.gdc.GameDayChannel;
import com.hazeluff.nhl.game.Game;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

public abstract class GDCSubCommand {

	protected static final String GAME_NOT_STARTED_MESSAGE = "The game hasn't started yet.";
	protected static final String GAME_STARTED_MESSAGE = "The game has already started.";


	public abstract String getName();

	public abstract String getDescription();

	public abstract Publisher<?> reply(ChatInputInteractionEvent event, NHLBot nhlBot, Game game);

	protected static GameDayChannel getGameDayChannel(ChatInputInteractionEvent event, NHLBot nhlBot, Game game) {
		return GDCCommand.getGameDayChannel(event, nhlBot, game);
	}
}
