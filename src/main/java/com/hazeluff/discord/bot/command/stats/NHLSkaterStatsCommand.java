package com.hazeluff.discord.bot.command.stats;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.reactivestreams.Publisher;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.Command;
import com.hazeluff.discord.nhl.NHLGateway;
import com.hazeluff.discord.nhl.Seasons.Season;
import com.hazeluff.discord.nhl.stats.SkaterStats;
import com.hazeluff.discord.nhl.stats.TeamPlayerStats;
import com.hazeluff.discord.utils.DiscordUtils;
import com.hazeluff.nhl.Team;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

public abstract class NHLSkaterStatsCommand extends NHLStatsSubCommand {
	protected Publisher<?> reply(ChatInputInteractionEvent event, NHLBot nhlBot, Predicate<SkaterStats> filter,
			String label) {
		Team team = Team.parse(DiscordUtils.getOptionAsString(event, "team"));
		// Default team
		if (team == null) {
			team = Team.VANCOUVER_CANUCKS;
		}
		Long startYear = DiscordUtils.getOptionAsLong(event, "season");
		Season season = getSeason(startYear);
		if (season.getStartYear() > Config.CURRENT_SEASON.getStartYear() || season.getStartYear() < 1917) {
			return Command.deferReply(event, "Season is out of range.");
		}

		TeamPlayerStats playerStats = NHLGateway.getTeamPlayerStats(team, season);
		List<SkaterStats> forwards = playerStats.getSkaters().stream()
				.filter(filter)
				.collect(Collectors.toList());
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("**" + label + " Stats**\n");
		stringBuilder.append(buildSkaterStatsTable(forwards));
		return Command.deferReply(event, stringBuilder.toString());
	}
	
	private static final String SKATER_TABLE_HEADERS = 
			"          Name             |GP |G  |A  |P  |+/-|PPG|SHG|GWG|S  |S%   |FO%";
	private static final String SKATER_TABLE_FORMAT = 
			"%-15s %-11s|%3s|%3s|%3s|%3s|%3s|%3s|%3s|%3s|%3s|%s|%s";

	static String buildSkaterStatsTable(List<SkaterStats> skaters) {
		StringBuilder stringBuilder = new StringBuilder("```\n");
		stringBuilder.append(SKATER_TABLE_HEADERS);
		for (SkaterStats stats : skaters) {
			stringBuilder.append("\n");
			String statLine = String.format(SKATER_TABLE_FORMAT, 
					stats.getLastName(), 
					stats.getFirstName(),
					stats.getGamesPlayed(),
					stats.getGoals(),
					stats.getAssists(),
					stats.getPoints(),
					stats.getPlusMinus(),
					stats.getPowerPlayGoals(),
					stats.getShorthandedGoals(),
					stats.getGameWinningGoals(),
					stats.getShots(),
					StringUtils.leftPad(String.format("%.2f", stats.getShootingPctg() * 100), 5),
					StringUtils.leftPad(String.format("%.2f", stats.getFaceoffWinPctg() * 100), 5)
			);
			stringBuilder.append(statLine);
		}
		stringBuilder.append("\n```");
		return stringBuilder.toString();
	}
}
