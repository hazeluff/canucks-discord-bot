package com.hazeluff.discord.bot.command.stats;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.Command;
import com.hazeluff.discord.nhl.NHLGateway;
import com.hazeluff.discord.nhl.Seasons.Season;
import com.hazeluff.discord.nhl.stats.TeamStandings;
import com.hazeluff.discord.utils.DiscordUtils;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.Team;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.InteractionFollowupCreateSpec;

public class NHLDivisionStatsCommand extends NHLStatsSubCommand {

	@Override
	public String getName() {
		return "division";
	}

	@Override
	public String getDescription() {
		return "Standings of a team's Division.";
	}

	private final AtomicReference<Map<Integer, String>> standingsSeasons = new AtomicReference<>();

	private Map<Integer, String> getStandingsSeasons() {
		// Cache Standings Seasons info.
		if (standingsSeasons.get() == null) {
			Map<Integer, String> fetchedStandingsSeasons = NHLGateway.getStandingsSeasonsEnds();
			if (fetchedStandingsSeasons != null) {
				standingsSeasons.updateAndGet(map -> fetchedStandingsSeasons);
			}
		}
		return standingsSeasons.get();
	}

	private static final String DIVISION_STATS_HEADERS = 
			"    Team                    |GP |W  |L  |OT |PTS|GF |GA |DIFF|  HOME  |  AWAY  | L-10 |STRK";
	private static final String DIVISION_STATS_FORMAT = 
			"%2s| %-24s|%3s|%3s|%3s|%3s|%3s|%3s|%3s|%4s|%8s|%8s|%6s|%2s%2s";

	@Override
	public Publisher<?> reply(ChatInputInteractionEvent event, NHLBot nhlBot) {
		Map<Integer, String> standingsSeasons = getStandingsSeasons();
		if (standingsSeasons == null) {
			return Command.reply(event, "Internal Error. (Could not get Standings Seasons)");
		}

		Season season = getSeason(DiscordUtils.getOptionAsLong(event, "season"));
		if (!standingsSeasons.containsKey(season.getStartYear())) {
			return Command.reply(event, "Season is out of range.");
		}
		
		return Command.replyAndDefer(event, "Fetching Results...", () -> buildFollowUp(event));
	}

	InteractionFollowupCreateSpec buildFollowUp(ChatInputInteractionEvent event) {
		Map<Integer, String> standingsSeasons = getStandingsSeasons();
		Season season = getSeason(DiscordUtils.getOptionAsLong(event, "season"));
		String endDate = standingsSeasons.get(season.getStartYear());
		List<TeamStandings> standings = NHLGateway.getStandings(endDate);

		// Determine the Division the team is in
		Team team = Team.parse(DiscordUtils.getOptionAsString(event, "team"));
		Team fTeam = team != null ? team : Team.VANCOUVER_CANUCKS;
		String division = Utils.getFromList(standings, stdng -> fTeam.equals(stdng.getTeam())).getDivisionName();

		List<TeamStandings> divisionStandings = standings.stream()
				.filter(standing -> division.equals(standing.getDivisionName()))
				.sorted((TeamStandings s1, TeamStandings s2) -> s1.getDivisionSequence() - s2.getDivisionSequence())
				.collect(Collectors.toList());

		return InteractionFollowupCreateSpec.builder()
				.content(buildReplyMessage(division, divisionStandings))
				.build();
	}

	static String buildReplyMessage(String division, List<TeamStandings> divisionStandings) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("**" + division + " Division Standings**\n");
		stringBuilder.append(buildStandingsTable(divisionStandings));
		return stringBuilder.toString();
	}

	static String buildStandingsTable(List<TeamStandings> standings) {
		StringBuilder stringBuilder = new StringBuilder("```\n");
		stringBuilder.append(DIVISION_STATS_HEADERS);
		for (TeamStandings standing : standings) {
			stringBuilder.append("\n");
			String statLine = String.format(DIVISION_STATS_FORMAT,
				standing.getDivisionSequence(),
				standing.getTeam().getFullName(),
				standing.getGamesPlayed(),
				standing.getWins(),
				standing.getLosses(),
				standing.getOtLosses(),
				standing.getPoints(),
				standing.getGoalFor(),
				standing.getGoalAgainst(),
				standing.getGoalDifferential(),
				standing.getHomeWins() + "-" + standing.getHomeLosses() + "-" + standing.getHomeOtLosses(),
				standing.getRoadWins() + "-" + standing.getRoadLosses() + "-" + standing.getRoadOtLosses(),
				standing.getL10Wins() + "-" + standing.getL10Losses() + "-" + standing.getL10OtLosses(),
					standing.getStreakCode(), standing.getStreakCount()
			);
			stringBuilder.append(statLine);
		}
		stringBuilder.append("\n```");
		return stringBuilder.toString();
	}
}
