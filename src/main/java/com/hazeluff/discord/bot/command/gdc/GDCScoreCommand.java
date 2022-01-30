package com.hazeluff.discord.bot.command.gdc;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.command.Command;
import com.hazeluff.nhl.game.Game;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.EmbedCreateSpec.Builder;

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
			return Command.deferReply(event, GAME_NOT_STARTED_MESSAGE);
		}

		return Command.deferReply(event, getEmbed(game));
	}

	public static EmbedCreateSpec getEmbed(Game game) {
		Builder embedBuilder = EmbedCreateSpec.builder();
		buildEmbed(embedBuilder, game);
		return embedBuilder.build();
	}
	
	public static EmbedCreateSpec.Builder buildEmbed(EmbedCreateSpec.Builder embedBuilder, Game game) {
		String homeGoals = "Goals:  **" + game.getHomeScore() + "**";
		String awayGoals = "Goals:  **" + game.getAwayScore() + "**";
		return embedBuilder
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
