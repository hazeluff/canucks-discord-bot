package com.hazeluff.discord.bot.command.stats;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.Command;
import com.hazeluff.discord.nhl.NHLSeasons.Season;
import com.hazeluff.discord.nhl.NHLTeams.Team;
import com.hazeluff.discord.utils.DiscordUtils;
import com.hazeluff.nhl.NHLGateway;
import com.hazeluff.nhl.stats.SkaterStats;
import com.hazeluff.nhl.stats.TeamPlayerStats;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

public abstract class NHLSkaterStatsCommand extends NHLStatsSubCommand {
	protected Publisher<?> reply(ChatInputInteractionEvent event, NHLBot nhlBot, String label,
			Predicate<SkaterStats> filter, Comparator<SkaterStats> order) {
		String strTeam = DiscordUtils.getOptionAsString(event, "team");
		if (!Team.isValid(strTeam)) {
			return event.reply(Command.getInvalidTeamCodeMessage(strTeam)).withEphemeral(true);
		}
		Team team = Team.parse(strTeam);
		// Default team
		if (team == null) {
			team = Team.VANCOUVER_CANUCKS;
		}
		// Only NHL Teams
		if (!team.isNHLTeam()) {
			return event.reply(Command.NON_NHL_TEAM_MESSAGE).withEphemeral(true);
		}

		Long startYear = DiscordUtils.getOptionAsLong(event, "season");
		Season season = getSeason(startYear);
		if (season.getStartYear() > Config.NHL_CURRENT_SEASON.getStartYear() || season.getStartYear() < 1917) {
			return Command.reply(event, "Season is out of range.");
		}

		TeamPlayerStats playerStats = NHLGateway.getTeamPlayerStats(team, season);
		List<SkaterStats> skaters = playerStats.getSkaters().stream()
				.filter(filter)
				.sorted(order)
				.collect(Collectors.toList());
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("**" + label + " Stats**\n");
		stringBuilder.append(buildSkaterStatsTable(skaters));
		return Command.reply(event, stringBuilder.toString());
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
