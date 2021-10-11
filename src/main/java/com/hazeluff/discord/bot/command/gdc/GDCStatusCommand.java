package com.hazeluff.discord.bot.command.gdc;

import org.reactivestreams.Publisher;

import com.hazeluff.nhl.Game;
import com.hazeluff.nhl.GameLiveData;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;

public class GDCStatusCommand extends GDCSubCommand {

	@Override
	public String getName() {
		return "score";
	}

	@Override
	public String getDescription() {
		return "Get the game's current score.";
	}

	@Override
	public Publisher<?> reply(ChatInputInteractionEvent event, Game game) {
		if (!game.getStatus().isStarted()) {
			return event.reply(GAME_NOT_STARTED_MESSAGE);
		}

		return event.reply(callbackSpec -> callbackSpec.addEmbed(embedSpec -> buildEmbed(embedSpec, game)));
	}

	public static EmbedCreateSpec buildEmbed(EmbedCreateSpec embedSpec, Game game) {
		embedSpec.setTitle("Status Report");
		embedSpec.setDescription(game.getHomeTeam().getFullName() + " vs " + game.getAwayTeam().getFullName());
		GameLiveData liveData = game.getLiveData();
		if (game.getStatus().isFinished()) {
			String description = "Game has finished in ";
			// TODO COmplete this
			if (liveData.getPeriod() > 3) {
				if (liveData.hasShootout()) {
					description += "shootout.";
				}
			}
			embedSpec.setDescription(description);
		}
		return embedSpec;
	}
}
