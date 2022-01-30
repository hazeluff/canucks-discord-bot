package com.hazeluff.discord.bot.command.gdc;

import org.reactivestreams.Publisher;

import com.hazeluff.nhl.game.Game;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

public abstract class GDCSubCommand {
	public abstract String getName();
	public abstract String getDescription();

	public abstract Publisher<?> reply(ChatInputInteractionEvent event, Game game);
	

	protected static final String GAME_NOT_STARTED_MESSAGE = "The game hasn't started yet.";

}
