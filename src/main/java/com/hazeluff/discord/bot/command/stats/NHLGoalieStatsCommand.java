package com.hazeluff.discord.bot.command.stats;

import java.util.List;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.Command;
import com.hazeluff.discord.nhl.NHLGateway;
import com.hazeluff.discord.nhl.Seasons;
import com.hazeluff.discord.nhl.Seasons.Season;
import com.hazeluff.discord.nhl.stats.GoalieStats;
import com.hazeluff.discord.nhl.stats.TeamPlayerStats;
import com.hazeluff.discord.utils.DiscordUtils;
import com.hazeluff.nhl.Team;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

public class NHLGoalieStatsCommand extends NHLStatsSubCommand {

	@Override
	public String getName() {
		return "goalies";
	}

	@Override
	public String getDescription() {
		return "Stats of Goalies.";
	}

	@Override
	public Publisher<?> reply(ChatInputInteractionEvent event, NHLBot nhlBot) {
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
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("**Goalie Stats**\n");
		stringBuilder.append(buildGoalieStatsTable(playerStats.getGoalies()));
		return Command.deferReply(event, stringBuilder.toString());
	}

	private static final String GOALIE_TABLE_HEADERS = 
			"          Name             |GP |W  |L  |OT|GAA |SV%  |SA  |SV  |GA |PIM|GS";
	private static final String GOALIE_TABLE_FORMAT = 
			"%-15s %-11s|%3s|%3s|%3s|%2s|%2.2f|%1.3f|%4s|%4s|%3s|%3s|%2s";

	static String buildGoalieStatsTable(List<GoalieStats> goalies) {
		StringBuilder stringBuilder = new StringBuilder("```\n");
		stringBuilder.append(GOALIE_TABLE_HEADERS);
		for (GoalieStats stats : goalies) {
			stringBuilder.append("\n");
			String statLine = String.format(GOALIE_TABLE_FORMAT, 
					stats.getLastName(), 
					stats.getFirstName(),
					stats.getGamesPlayed(),
					stats.getWins(),
					stats.getLosses(),
					stats.getOvertimeLosses(),
					stats.getGoalsAgainstAverage(),
					stats.getSavePercentage(),
					stats.getShotsAgainst(),
					stats.getSaves(),
					stats.getGoalsAgainst(),
					stats.getPenaltyMinutes(),
					stats.getGamesStarted()
			);
			stringBuilder.append(statLine);
		}
		stringBuilder.append("\n```");
		return stringBuilder.toString();
	}

	/**
	 * Gets a season given a season code or value.
	 * e.g.
	 * 99 -> 1999,
	 * 20 -> 2020
	 * 
	 * @param startYear
	 * @return
	 */
	static Season getSeason(Long startYear) {
		if (startYear != null) {
			int year = startYear.intValue();
			if (year >= 0 && year < (Config.CURRENT_SEASON.getStartYear() - 2000)) {
				year = year + 2000;
			} else if (year >= (Config.CURRENT_SEASON.getStartYear() - 2000) && year < 100) {
				year = year + 1900;
			}
			return Season.mock(year);
		}
		return Config.CURRENT_SEASON;
	}

	public static void main(String[] argv) {
		TeamPlayerStats playerStats = NHLGateway.getTeamPlayerStats(Team.VANCOUVER_CANUCKS, Seasons.S22_23);
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("**Goalie Stats**\n");
		stringBuilder.append(buildGoalieStatsTable(playerStats.getGoalies()));
		System.out.println(stringBuilder.toString().length());
	}
}
