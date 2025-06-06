package com.hazeluff.discord.bot.command.stats;

import java.util.List;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.Command;
import com.hazeluff.discord.nhl.NHLSeasons.Season;
import com.hazeluff.discord.nhl.NHLTeams.Team;
import com.hazeluff.discord.utils.DiscordUtils;
import com.hazeluff.nhl.NHLGateway;
import com.hazeluff.nhl.stats.GoalieStats;
import com.hazeluff.nhl.stats.TeamPlayerStats;

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
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("**Goalie Stats**\n");
		stringBuilder.append(buildGoalieStatsTable(playerStats.getGoalies()));
		return Command.reply(event, stringBuilder.toString());
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
}
