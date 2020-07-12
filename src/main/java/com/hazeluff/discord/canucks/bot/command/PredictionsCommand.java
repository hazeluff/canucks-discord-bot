package com.hazeluff.discord.canucks.bot.command;

import java.util.List;
import java.util.function.Consumer;

import com.hazeluff.discord.canucks.Config;
import com.hazeluff.discord.canucks.bot.CanucksBot;
import com.hazeluff.discord.canucks.bot.database.predictions.campaigns.PredictionsScore;
import com.hazeluff.discord.canucks.bot.database.predictions.campaigns.SeasonCampaign;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;

/**
 * Displays information about CanucksBot and the author
 */
public class PredictionsCommand extends Command {

	public PredictionsCommand(CanucksBot canucksBot) {
		super(canucksBot);
	}

	@Override
	public void execute(MessageCreateEvent event, List<String> arguments) {		
		long userId = event.getMember().get().getId().asLong();
				
		PredictionsScore score = SeasonCampaign.getScore(canucksBot, Config.SEASON_YEAR_END, userId);
		String message;
		if(score == null) {
			message = "[Internal Error] Required database did not have results for the season.";
		} else {
			String place = "x'th";

			message = String.format("You placed %s.\n"
					+ "You predicted %s games correctly out of %s. There are/were a total of %s games to predict on.",
					place, score.getNumCorrect(), score.getTotalPredictions(), score.getTotalGames());
		}
		sendMessage(event, spec -> spec.setContent(message));
	}

	public Consumer<MessageCreateSpec> getReply() {

		return spec -> spec.setContent("");
	}

	@Override
	public boolean isAccept(Message message, List<String> arguments) {
		return arguments.get(0).equalsIgnoreCase("predictions");
	}
}