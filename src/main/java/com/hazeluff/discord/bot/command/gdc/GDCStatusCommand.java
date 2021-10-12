package com.hazeluff.discord.bot.command.gdc;

import org.reactivestreams.Publisher;

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

		return event.reply(callbackSpec -> callbackSpec.addEmbed(embedSpec -> buildEmbed(embedSpec, game)));
	}

	public static EmbedCreateSpec buildEmbed(EmbedCreateSpec embedSpec, Game game) {
		embedSpec.setTitle("Status Report");
		embedSpec.setDescription(
				String.format("%s vs %s", game.getHomeTeam().getFullName(), game.getAwayTeam().getFullName()));
		embedSpec.setFooter("Status: " + game.getStatus().getDetailedState().toString(), null);

		String description;
		if (game.getStatus().isFinished()) {
			description = "Game has finished";
			if (game.getLineScore().hasShootout()) {
				description += " in shootout.";
				// Add shootout score
			}
			int numOvertime = game.getLineScore().getCurrentPeriod() - 3;
			if (numOvertime <= 0) {
				description += " in regulation time.";

			} else if (numOvertime == 1) {
				description += " in overtime";
			} else {
				description += " in " + numOvertime + " overtimes";
			}
		} else if (game.getStatus().isStarted()) {
			description = "Game is in progress.";
			if (game.getLineScore().hasShootout()) {
				description += " Currently in shootout.";
				// Add shootout score
			}
			int numOvertime = game.getLineScore().getCurrentPeriod() - 3;
			if (numOvertime <= 0) {
				description += " Currently in " + game.getLineScore().getCurrentPeriod() + " period.";
			} else if (numOvertime == 1) {
				description += " Currently in overtime.";
			} else {
				description += " Currently in " + numOvertime + " overtimes";
			}
		} else {
			description = "Game has not started.";
		}

		String score = String.format("%s %s - %s %s",
				game.getHomeTeam().getName(), game.getHomeScore(),
				game.getAwayScore(), game.getAwayTeam().getName());
		embedSpec.addField(description, score, false);
		
		if (game.getStatus().isLive()) {
			if (game.getLineScore().isIntermission()) {
				String intermissionTitle = "Currently in an intermission: "
						+ game.getLineScore().getCurrentPeriodOrdinal();
				String intermissionDescription = String.format("Elapsed: %s. Remaining: %s.",
						game.getLineScore().getIntermissionTimeElapsed(),
						game.getLineScore().getIntermissionTimeRemaining());
				embedSpec.addField(intermissionTitle, intermissionDescription, false);
			}
		}
		return embedSpec;
	}
}
