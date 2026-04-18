package com.hazeluff.discord.bot.database.preferences;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;

import com.hazeluff.discord.nhl.NHLTeams.Team;

public class GuildPreferences {
	private Set<Team> teams;
	private Long gdcChannelId;
	private Long playoffChannelId;
	private boolean useChannelThreads; // gdcChannelId must be set;
	// false - post updates in GDC Channel; true - send updates in GDC thread

	public GuildPreferences() {
		this.teams = new HashSet<>();
		gdcChannelId = null;
		playoffChannelId = null;
		useChannelThreads = false;
	}

	private GuildPreferences(Set<Team> teams, Long gdcChannelId, Long playoffChannelId, boolean useThreads) {
		this.teams = teams;
		this.gdcChannelId = gdcChannelId;
		this.playoffChannelId = playoffChannelId;
		this.useChannelThreads = useThreads;
	}

	@SuppressWarnings("unchecked")
	public static GuildPreferences parse(Document doc) {
		Set<Team> teams;
		if (doc.containsKey("teams")) {
			teams = ((List<Integer>) doc.get("teams")).stream().map(Team::parse).collect(Collectors.toSet());
		} else {
			teams = new HashSet<>();
		}

		Long gdcChannelId = doc.getLong("gdcChannelId");
		Long playoffChannelId = doc.getLong("playoffChannelId");
		boolean useThreads = doc.getBoolean("useChannelThreads", false);

		return new GuildPreferences(teams, gdcChannelId, playoffChannelId, useThreads);
	}

	public List<Team> getTeams() {
		return new ArrayList<>(teams);
	}

	public void addTeam(Team team) {
		teams.add(team);
	}

	public void removeTeam(Team team) {
		teams.remove(team);
	}

	public String getCheer() {
		if (teams.size() > 1) {
			return Team.MULTI_TEAM_CHEER;
		} else {
			Team team = teams.iterator().next();
			return team == null ? Team.MULTI_TEAM_CHEER : team.getCheer();
		}
	}

	public ZoneId getTimeZone() {
		if (teams.size() > 1) {
			return ZoneId.of("America/Toronto");
		} else {
			return teams.iterator().next().getTimeZone();
		}
	}

	public void setGameDayChannelId(Long channelId) {
		this.gdcChannelId = channelId;
	}

	public void setPlayoffChannelId(Long channelId) {
		this.playoffChannelId = channelId;
	}

	public Long getGameDayChannelId() {
		return gdcChannelId;
	}

	public Long getPlayoffChannelId() {
		return playoffChannelId;
	}

	public boolean isUseChannelThreads() {
		return useChannelThreads;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((gdcChannelId == null) ? 0 : gdcChannelId.hashCode());
		result = prime * result + ((teams == null) ? 0 : teams.hashCode());
		result = prime * result + (useChannelThreads ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GuildPreferences other = (GuildPreferences) obj;
		if (gdcChannelId == null) {
			if (other.gdcChannelId != null)
				return false;
		} else if (!gdcChannelId.equals(other.gdcChannelId))
			return false;
		if (teams == null) {
			if (other.teams != null)
				return false;
		} else if (!teams.equals(other.teams))
			return false;
		if (useChannelThreads != other.useChannelThreads)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "GuildPreferences [teams=" + teams + "]";
	}

}
