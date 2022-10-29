package com.hazeluff.discord.bot.command.gdc;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.Command;
import com.hazeluff.discord.bot.database.predictions.campaigns.SeasonCampaign;
import com.hazeluff.discord.bot.database.predictions.campaigns.SeasonCampaign.Prediction;
import com.hazeluff.nhl.Team;
import com.hazeluff.nhl.game.Game;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

public abstract class GDCVoteCommand extends GDCSubCommand {

	protected Publisher<?> replyVoteTeam(ChatInputInteractionEvent event, NHLBot nhlBot, Game game, Team team) {
		if (game.getStatus().isStarted()) {
			return Command.deferReply(event, GAME_STARTED_MESSAGE, true);
		}

		String campaignId = SeasonCampaign.buildCampaignId(Config.CURRENT_SEASON.getAbbreviation());
		Long userId = event.getInteraction().getUser().getId().asLong();
		Prediction prediction = new Prediction(campaignId, userId, game.getGamePk(), team.getId());
		SeasonCampaign.savePrediction(nhlBot, prediction);

		new Thread(() -> getGameDayChannel(event, nhlBot, game).updateVotingMessage()).start();

		String userName = event.getInteraction().getUser().getUsername();
		String message = String.format("`%s` has voted for the %s to win!", 
				userName, team.getName());
		return Command.deferReply(event, message, false);

	}
}
