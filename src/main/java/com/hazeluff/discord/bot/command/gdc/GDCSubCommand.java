package com.hazeluff.discord.bot.command.gdc;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.nhl.game.Game;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

public abstract class GDCSubCommand {
	protected static final String GAME_STARTED_MESSAGE = "The game has already started.";


	public abstract String getName();

	public abstract String getDescription();

	public abstract Publisher<?> reply(ChatInputInteractionEvent event, NHLBot nhlBot, Game game);

	public static String BuildGameNotStartedMessage(Game game) {
		String time = game.isStartTimeTBD()
				? "`TBD`"
				: String.format("<t:%s>", game.getStartTime().toEpochSecond());
		return "The game hasn't started yet. It starts at " + time + ".";
	}
}
