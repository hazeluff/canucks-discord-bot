package com.hazeluff.discord.bot.command.gdc;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.Command;
import com.hazeluff.discord.bot.database.predictions.campaigns.SeasonCampaign;
import com.hazeluff.discord.bot.database.predictions.campaigns.SeasonCampaign.Prediction;
import com.hazeluff.nhl.game.Game;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

public class GDCVoteHomeCommand extends GDCSubCommand {

	@Override
	public String getName() {
		return "votehome";
	}

	@Override
	public String getDescription() {
		return "Vote for the Home team to win!";
	}

	@Override
	public Publisher<?> reply(ChatInputInteractionEvent event, NHLBot nhlBot, Game game) {
		if (game.getStatus().isStarted()) {
			return Command.deferReply(event, GAME_STARTED_MESSAGE, true);
		}

		String campaignId = SeasonCampaign.buildCampaignId(Config.CURRENT_SEASON.getAbbreviation());
		Long userId = event.getInteraction().getUser().getId().asLong();
		Prediction prediction = new Prediction(campaignId, userId, game.getGamePk(), game.getHomeTeam().getId());
		SeasonCampaign.savePrediction(nhlBot, prediction);

		String message = String.format("You have voted for the Home team (%s) to win!", game.getHomeTeam().getName());
		return Command.deferReply(event, message, true);
	}
}
