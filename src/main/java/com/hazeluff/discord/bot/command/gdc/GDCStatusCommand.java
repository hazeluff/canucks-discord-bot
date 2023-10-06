package com.hazeluff.discord.bot.command.gdc;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
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
	public Publisher<?> reply(ChatInputInteractionEvent event, NHLBot nhlBot, Game game) {
		if (!game.getGameState().isStarted()) {
			return event.reply(GAME_NOT_STARTED_MESSAGE);
		}

		return Command.reply(event, buildEmbed(game));
	}

	public static EmbedCreateSpec buildEmbed(Game game) {
		EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();
		embedBuilder.title("Status Report");
		embedBuilder.description(
				String.format("%s vs %s", game.getHomeTeam().getFullName(), game.getAwayTeam().getFullName()));
		embedBuilder.footer("Status: " + game.getGameState(), null);

		String fieldDescription;
		if (game.getGameState().isFinal()) {
			fieldDescription = "Game has finished";
			if (game.hasShootout()) {
				fieldDescription += " in shootout.";
				// Add shootout score
			} else {
				int numOvertime = game.getPeriod() - 3;
				if (numOvertime <= 0) {
					fieldDescription += " in regulation time.";

				} else if (numOvertime == 1) {
					fieldDescription += " in overtime";
				} else {
					fieldDescription += " in " + numOvertime + " overtimes";
				}
			}

		} else if (game.getGameState().isStarted()) {
			fieldDescription = "Game is in progress.";
			if (game.hasShootout()) {
				fieldDescription += " Currently in shootout.";
				// Add shootout score
			} else {
				int numOvertime = game.getPeriod() - 3;
				if (numOvertime <= 0) {
					if (!game.isInIntermission()) {
						fieldDescription += " Currently in "
								+ game.getPeriodOridnal() + " period.";						
					} else {
						fieldDescription += " Currently in intermission after the "
								+ game.getPeriodOridnal() + " period.";
					}
				} else if (numOvertime == 1) {
					fieldDescription += " Currently in overtime.";
				} else {
					fieldDescription += " Currently in " + numOvertime + " overtimes";
				}
			}
		} else {
			// Might not display as main command will reply when games are not started.
			fieldDescription = "Game has not started.";
		}

		String score = String.format("%s %s - %s %s",
				game.getHomeTeam().getName(), game.getHomeScore(),
				game.getAwayScore(), game.getAwayTeam().getName());
		embedBuilder.addField(fieldDescription, score, false);
		
		if (game.getGameState().isLive()) {
			if (game.isInIntermission()) {
				String intermissionTitle = "Currently in an intermission: " + game.getPeriodOridnal();
				String intermissionDescription = String.format("Remaining: %s.", game.getClockRemaining());
				embedBuilder.addField(intermissionTitle, intermissionDescription, false);
			}
		}
		return embedBuilder.build();
	}
}
