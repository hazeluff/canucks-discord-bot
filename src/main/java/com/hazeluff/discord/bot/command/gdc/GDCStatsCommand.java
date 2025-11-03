package com.hazeluff.discord.bot.command.gdc;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.Command;
import com.hazeluff.nhl.game.Game;
import com.hazeluff.nhl.game.TeamGameStats;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;

public class GDCStatsCommand extends GDCSubCommand {

	@Override
	public String getName() {
		return "stats";
	}

	@Override
	public String getDescription() {
		return "Team Stats.";
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
		embedBuilder.title("Team Statistics");
		StringBuilder statNameCol = new StringBuilder("~~");
		StringBuilder homeCol = new StringBuilder("Home");
		StringBuilder awayCol = new StringBuilder("Away");
		StringBuilder[] builders = new StringBuilder[] { statNameCol, homeCol, awayCol };
		
		TeamGameStats stats = game.getTeamGameStats();
		addStat(builders, "SOG", stats.getSog());
		addStat(builders, "FO%", stats.getFoWPct());
		addStat(builders, "PP", stats.getPp());
		addStat(builders, "PP%", stats.getPpPct());
		addStat(builders, "Hits", stats.getHits());
		addStat(builders, "Blocks", stats.getBlocked());
		addStat(builders, "Giveaways", stats.getGiveaways());
		addStat(builders, "Takeaways", stats.getTakeaways());

		embedBuilder
				.addField(
						game.getHomeTeam().getName(),
						homeCol.toString(),
						true
				)
				.addField(
						"vs",
						statNameCol.toString(),
						true
				)
				.addField(
						game.getAwayTeam().getName(),
						awayCol.toString(),
						true
				);
		return embedBuilder.build();
	}
	
	private static void addStat(StringBuilder[] builders, String statName, String[] values) {
		addStat(builders, statName, values[0], values[1]);
	}

	private static void addStat(StringBuilder[] builders, String statName, int[] values) {
		addStat(builders, statName, String.valueOf(values[0]), String.valueOf(values[1]));
	}

	private static void addStat(StringBuilder[] builders, String statName, double[] values) {
		addStat(builders, statName, String.format("%.1f", values[0] * 100.0f), String.format("%.1f", values[1] * 100.0f));
	}

	private static void addStat(StringBuilder[] builders, String statName, String homeValue, String awayValue) {
		builders[0].append("\n").append(statName);
		builders[1].append("\n").append(homeValue);
		builders[2].append("\n").append(awayValue);
	}
}
