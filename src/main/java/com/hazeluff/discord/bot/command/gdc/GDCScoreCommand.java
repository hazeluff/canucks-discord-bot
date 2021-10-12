package com.hazeluff.discord.bot.command.gdc;

import org.reactivestreams.Publisher;

import com.hazeluff.nhl.game.Game;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;

public class GDCScoreCommand extends GDCSubCommand {

	@Override
	public String getName() {
		return "score";
	}

	@Override
	public String getDescription() {
		return "Current score.";
	}

	@Override
	public Publisher<?> reply(ChatInputInteractionEvent event, Game game) {
		if (!game.getStatus().isStarted()) {
			return event.reply(GAME_NOT_STARTED_MESSAGE);
		}

		return event.reply(callbackSpec -> callbackSpec.addEmbed(embedSpec -> buildEmbed(embedSpec, game)));
	}
	
	public static EmbedCreateSpec buildEmbed(EmbedCreateSpec embedSpec, Game game) {
		String homeGoals = "Goals:  **" + game.getHomeScore() + "**";
		String awayGoals = "Goals:  **" + game.getAwayScore() + "**";
		return embedSpec
				.addField(
						game.getHomeTeam().getFullName(),
						"Home\n" + homeGoals,
						true
				)
				.addField(
						"vs",
						"~~", // For formatting
						true
				)
				.addField(
						game.getAwayTeam().getFullName(),
						"Away\n" + awayGoals,
						true
				);
	}

}
