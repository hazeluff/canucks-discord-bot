package com.hazeluff.discord.bot.command.gdc;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
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
	public Publisher<?> reply(ChatInputInteractionEvent event, NHLBot nhlBot, Game game) {
		if (!game.getGameState().isStarted()) {
			return Command.reply(event, GAME_NOT_STARTED_MESSAGE);
		}

		return Command.reply(event, getEmbed(game));
	}

	public static EmbedCreateSpec getEmbed(Game game) {
		Builder embedBuilder = EmbedCreateSpec.builder();
		buildEmbed(embedBuilder, game);
		return embedBuilder.build();
	}
	
	public static EmbedCreateSpec.Builder buildEmbed(EmbedCreateSpec.Builder embedBuilder, Game game) {
		String homeTeam = game.getGameType().isFourNations()
				? game.getHomeTeam().getLocation()
				: game.getHomeTeam().getFullName();
		String homeGoals = "Goals:  **" + game.getHomeScore() + "**";
		String awayTeam = game.getGameType().isFourNations()
				? game.getAwayTeam().getLocation()
				: game.getAwayTeam().getFullName();
		String awayGoals = "Goals:  **" + game.getAwayScore() + "**";
		return embedBuilder
				.addField(
						homeTeam,
						"Home\n" + homeGoals,
						true
				)
				.addField(
						"vs",
						"~~", // For formatting
						true
				)
				.addField(
						awayTeam,
						"Away\n" + awayGoals,
						true
				);
	}

}
