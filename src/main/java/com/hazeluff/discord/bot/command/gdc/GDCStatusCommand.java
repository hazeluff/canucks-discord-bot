package com.hazeluff.discord.bot.command.gdc;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.command.Command;
import com.hazeluff.nhl.game.Game;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;

public class GDCStatusCommand extends GDCSubCommand {

	@Override
	public String getName() {
		return "status";
	}

	@Override
	public String getDescription() {
		return "Game/Intermission/Power Play statuses.";
	}

	@Override
	public Publisher<?> reply(ChatInputInteractionEvent event, Game game) {
		if (!game.getStatus().isStarted()) {
			return event.reply(GAME_NOT_STARTED_MESSAGE);
		}

		return Command.deferReply(event, buildEmbed(game));
	}

	public static EmbedCreateSpec buildEmbed(Game game) {
		EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();
		embedBuilder.title("Status Report");
		embedBuilder.description(
				String.format("%s vs %s", game.getHomeTeam().getFullName(), game.getAwayTeam().getFullName()));
		embedBuilder.footer("Status: " + game.getStatus().getDetailedState().toString(), null);

		String fieldDescription;
		if (game.getStatus().isFinished()) {
			fieldDescription = "Game has finished";
			if (game.getLineScore().hasShootout()) {
				fieldDescription += " in shootout.";
				// Add shootout score
			}
			int numOvertime = game.getLineScore().getCurrentPeriod() - 3;
			if (numOvertime <= 0) {
				fieldDescription += " in regulation time.";

			} else if (numOvertime == 1) {
				fieldDescription += " in overtime";
			} else {
				fieldDescription += " in " + numOvertime + " overtimes";
			}
		} else if (game.getStatus().isStarted()) {
			fieldDescription = "Game is in progress.";
			if (game.getLineScore().hasShootout()) {
				fieldDescription += " Currently in shootout.";
				// Add shootout score
			}
			int numOvertime = game.getLineScore().getCurrentPeriod() - 3;
			if (numOvertime <= 0) {
				fieldDescription += " Currently in " + game.getLineScore().getCurrentPeriod() + " period.";
			} else if (numOvertime == 1) {
				fieldDescription += " Currently in overtime.";
			} else {
				fieldDescription += " Currently in " + numOvertime + " overtimes";
			}
		} else {
			// Might not display as main command will reply when games are not started.
			fieldDescription = "Game has not started.";
		}

		String score = String.format("%s %s - %s %s",
				game.getHomeTeam().getName(), game.getHomeScore(),
				game.getAwayScore(), game.getAwayTeam().getName());
		embedBuilder.addField(fieldDescription, score, false);
		
		if (game.getStatus().isLive()) {
			if (game.getLineScore().isIntermission()) {
				String intermissionTitle = "Currently in an intermission: "
						+ game.getLineScore().getCurrentPeriodOrdinal();
				String intermissionDescription = String.format("Elapsed: %s. Remaining: %s.",
						game.getLineScore().getIntermissionTimeElapsed(),
						game.getLineScore().getIntermissionTimeRemaining());
				embedBuilder.addField(intermissionTitle, intermissionDescription, false);
			}
		}
		return embedBuilder.build();
	}
}
