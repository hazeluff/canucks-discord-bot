package com.hazeluff.discord.bot.command.stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
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

public class NHLWildcardStatsCommand extends NHLStatsSubCommand {

	@Override
	public String getName() {
		return "wildcard";
	}

	@Override
	public String getDescription() {
		return "Standings of a team's Wildcard contention (Conference).";
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

		return Command.replyAndDefer(event, "Fetching Results...", buildFollowupSpecSupplier(event));
	}

	Supplier<InteractionFollowupCreateSpec> buildFollowupSpecSupplier(ChatInputInteractionEvent event) {
		return () -> {
			Map<Integer, String> standingsSeasons = getStandingsSeasons();
			Season season = getSeason(DiscordUtils.getOptionAsLong(event, "season"));
			String endDate = standingsSeasons.get(season.getStartYear());
			List<TeamStandings> standings = NHLGateway.getStandings(endDate);

			String strTeam = DiscordUtils.getOptionAsString(event, "team");
			if (!Team.isValid(strTeam)) {
				return InteractionFollowupCreateSpec.builder()
					.content(Command.getInvalidTeamCodeMessage(strTeam))
					.ephemeral(true)
					.build();
			}
			Team team = Team.parse(strTeam);
			// Default team
			if (team == null) {
				team = Team.VANCOUVER_CANUCKS;
			}
			// Only NHL Teams
			if (!team.isNHLTeam()) {
				return InteractionFollowupCreateSpec.builder().content(Command.NON_NHL_TEAM_MESSAGE).ephemeral(true)
						.build();
			}

			// Determine the Division the team is in
			Team fTeam = team;
			String conference = Utils.getFromList(standings, stdng -> fTeam.equals(stdng.getTeam()))
					.getConferenceName();

			List<TeamStandings> conferenceStandings = standings.stream()
					.filter(standing -> conference.equals(standing.getConferenceName()))					
					.collect(Collectors.toList());

			String message = buildReplyMessage(conference, conferenceStandings);

			return InteractionFollowupCreateSpec.builder()
					.content(message)
					.build();
		};
	}
	
	static String buildReplyMessage(String conference, List<TeamStandings> conferenceStandings) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("**" + StringUtils.capitalize(conference) + " Wildcard Standings**\n");
		Map<String, List<TeamStandings>> divisions = getDivisionTeams(conferenceStandings);
		for (Entry<String, List<TeamStandings>> entry : divisions.entrySet()) {
			String confName = entry.getKey();
			List<TeamStandings> divisionStandings = entry.getValue().stream()
				.sorted(
						(TeamStandings s1, TeamStandings s2) -> 
						s1.getDivisionSequence() - s2.getDivisionSequence()
				)
				.collect(Collectors.toList());
			stringBuilder.append("\n" + confName + " Division\n");
			stringBuilder.append(buildStandingsTable(divisionStandings));
		}

		List<TeamStandings> wildcardStandings = conferenceStandings.stream()
			.filter(standing -> standing.getWildcardSequence() > 0)
			.sorted(
				(TeamStandings s1, TeamStandings s2) -> 
				s1.getWildcardSequence() - s2.getWildcardSequence()
			)
			.collect(Collectors.toList());

		stringBuilder.append("\nWildcards\n");
		stringBuilder.append(buildStandingsTable(wildcardStandings));

		stringBuilder.append(
				"\n`x - Clinched Playoff spot; "
				+ "y - Clinched Division; "
				+ "p - President's Trophy; "
				+ "z - Clinched Conference`");
		
		return stringBuilder.toString();
	}

	/**
	 * Splits a list of standings and creates a map that separate the lists them by
	 * Division.
	 * 
	 * @param conferenceStandings
	 * @return
	 */
	static Map<String, List<TeamStandings>> getDivisionTeams(List<TeamStandings> standings) {
		Map<String, List<TeamStandings>> result = new HashMap<>();

		BiConsumer<Map<String, List<TeamStandings>>, TeamStandings> addToMap = (map, standing) -> {
			String divName = standing.getDivisionName();
			if (!map.containsKey(divName)) {
				map.put(divName, new ArrayList<>());
			}
			map.get(divName).add(standing);
		};

		for(TeamStandings standing : standings) {
			if (standing.getWildcardSequence() <= 0) {
				addToMap.accept(result, standing);
			}
		}
		return result;
	}

	private static final String WILDCARD_STATS_HEADERS = 
			"       Team                  |GP |W  |L  |OT |PTS|DIFF| L-10 |STRK";
	private static final String WILDCARD_STATS_FORMAT = 
			" %1s | %-24s|%3s|%3s|%3s|%3s|%3s|%4s|%6s|%2s%2s";

	static String buildStandingsTable(List<TeamStandings> standings) {
		StringBuilder stringBuilder = new StringBuilder("```\n");
		stringBuilder.append(WILDCARD_STATS_HEADERS);
		for (TeamStandings standing : standings) {
			stringBuilder.append("\n");
			String statLine = String.format(WILDCARD_STATS_FORMAT,
				standing.getClinchIndicator(),
				standing.getTeam().getFullName(),
				standing.getGamesPlayed(),
				standing.getWins(),
				standing.getLosses(),
				standing.getOtLosses(),
				standing.getPoints(),
				standing.getGoalDifferential(),
				standing.getL10Wins() + "-" + standing.getL10Losses() + "-" + standing.getL10OtLosses(),
				standing.getStreakCode(), standing.getStreakCount()
			);
			stringBuilder.append(statLine);
		}
		stringBuilder.append("\n```");
		return stringBuilder.toString();
	}
}
