package com.hazeluff.discord.bot.command.gdc;

import java.util.function.Consumer;

import org.reactivestreams.Publisher;

import com.hazeluff.nhl.game.Game;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;

public abstract class GDCSubCommand {
	public abstract String getName();
	public abstract String getDescription();

	public abstract Publisher<?> reply(ChatInputInteractionEvent event, Game game);
	

	protected static final Consumer<? super InteractionApplicationCommandCallbackSpec> GAME_NOT_STARTED_MESSAGE = 
			callbackSpec -> callbackSpec
					.setContent("The game hasn't started yet.")
					.setEphemeral(true);

}
