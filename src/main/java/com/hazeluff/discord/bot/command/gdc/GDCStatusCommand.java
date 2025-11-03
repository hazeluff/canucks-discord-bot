package com.hazeluff.discord.bot.command.gdc;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.Command;
import com.hazeluff.discord.utils.Utils;
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
		return "Start Time/Game/Intermission/Power Play statuses.";
	}

	@Override
	public Publisher<?> reply(ChatInputInteractionEvent event, NHLBot nhlBot, Game game) {
		if (!game.getGameState().isStarted()) {
			return event.reply(BuildGameNotStartedMessage(game));
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
		if (game.getGameState().isFinished()) {
			fieldDescription = "Game has finished";
			switch(game.getPeriodType()) {
				case REGULAR:
					fieldDescription += " in regulation time.";
					break;
				case OVERTIME:
					int numOvertime = game.getPeriodNumber() - 3;
					if (numOvertime == 1) {
						fieldDescription += " in overtime.";
					} else {
						fieldDescription += " in " + numOvertime + " overtimes.";
					}
					break;
				case SHOOTOUT:
					fieldDescription += " in shootout.";
					break;
				default:
					fieldDescription += ".";
			}
		} else if (game.getGameState().isStarted()) {
			fieldDescription = "Game is in progress.";
			switch (game.getPeriodType()) {
			case REGULAR:
				fieldDescription += " Currently in the " + Utils.getOrdinal(game.getPeriodNumber()) + " period.";
				break;
			case OVERTIME:
				int numOvertime = game.getPeriodNumber() - 3;

				if (numOvertime == 1) {
					fieldDescription += " Currently in overtime.";
				} else {
					fieldDescription += " Currently in " + Utils.getOrdinal(numOvertime) + " overtime.";
				}
				break;
			case SHOOTOUT:
				fieldDescription += " Currently in shootout.";
				break;
			default:
				fieldDescription += ".";
			}
		} else {
			// Might not display as main command will reply when games are not started.
			fieldDescription = "Game has not started.";
		}

		// Append Score Information
		String score = String.format("%s %s - %s %s",
				game.getHomeTeam().getName(), game.getHomeScore(),
				game.getAwayScore(), game.getAwayTeam().getName());
		embedBuilder.addField(fieldDescription, score, false);
		
		// Append Intermission Status
		if (game.getGameState().isLive()) {
			if (game.isInIntermission()) {
				String intermissionTitle = "Currently in an intermission";
				String intermissionDescription = String.format("Remaining: %s.", game.getClockRemaining());
				embedBuilder.addField(intermissionTitle, intermissionDescription, false);
			}
		}
		return embedBuilder.build();
	}
}
