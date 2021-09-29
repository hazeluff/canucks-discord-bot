package com.hazeluff.discord.bot.command;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.predictions.campaigns.PredictionsScore;
import com.hazeluff.discord.bot.database.predictions.campaigns.SeasonCampaign;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Displays information about NHLBot and the author
 */
public class PredictionsCommand extends Command {
	static final String NAME = "predictions";

	public PredictionsCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	public String getName() {
		return NAME;
	}

	public ApplicationCommandRequest getACR() {
		return null;
	}

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {
		// Personal Score
		if (event.getOption("self").get().getValue().get().asBoolean()) {

			long userId = event.getInteraction().getUser().getId().asLong();

			PredictionsScore score = SeasonCampaign.getScore(nhlBot, Config.CURRENT_SEASON.getAbbreviation(),
					userId);
			if (score == null) {
				return event.reply(NO_RESULTS);
			}

			String place = "x'th";

			// TODO Implment logic to get your place

			String message = String.format("You placed %s.\n"
					+ "You predicted %s games correctly out of %s. There are/were a total of %s games to predict on.",
					place, score.getNumCorrect(), score.getTotalPredictions(), score.getTotalGames());
			return event.replyEphemeral(message);
		}
		
		// Server Ranking/Scores
		List<Pair<Long, Integer>> playerRankings = 
				SeasonCampaign.getRankings(nhlBot, Config.CURRENT_SEASON.getAbbreviation());
		
		StringBuilder messageBuilder = new StringBuilder("Here are the results for Season Predictions:\n");
		messageBuilder.append("```");
		int listedNameLength = 32;
		for (Pair<Long, Integer> userRanking : playerRankings) {
			long userId = userRanking.getValue0();
			int score = userRanking.getValue1();
			User user = nhlBot.getDiscordManager().getUser(userId);
			String listedUserName = user != null 
					? getAndPadUserName(user, listedNameLength)
					: getInvalidUserName(userId, listedNameLength);
						
			messageBuilder.append(String.format("%s %s", listedUserName, score));
			messageBuilder.append("\n");
		}
		messageBuilder.append("```");
		return event.reply(messageBuilder.toString());
	}

	static String NO_RESULTS = "[Internal Error] Required database did not have results for the season.";

	static String getAndPadUserName(User user, int length) {
		String userName = user.getUsername();
		String discriminator = "#" + user.getDiscriminator();
		if (userName.length() >= length - discriminator.length() - 2) {
			userName = userName.substring(0, length - discriminator.length() - 2) + "..";
		}
		return StringUtils.rightPad(userName + discriminator, length, " ");
	}

	static String getInvalidUserName(long userId, int length) {
		String strUserId = String.valueOf(userId);
		int addedCharLength = "unknown()".length() + "..".length();
		if (strUserId.length() > length - addedCharLength) {
			strUserId = strUserId.substring(0, length - addedCharLength) + "..";
		}
		return StringUtils.rightPad(String.format("unknown(%s)", strUserId), length);
	}
}
