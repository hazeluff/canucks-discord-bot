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
	private GDCMode gdcMode;
	private PlayoffMode playoffMode;

	public GuildPreferences() {
		this.teams = new HashSet<>();
		this.gdcChannelId = null;
		this.playoffChannelId = null;
		this.gdcMode = null;
		this.playoffMode = null;
	}

	private GuildPreferences(Set<Team> teams, Long gdcChannelId, Long playoffChannelId,
		GDCMode gdcMode, PlayoffMode playoffMode) {
		this.teams = teams;
		this.gdcChannelId = gdcChannelId;
		this.playoffChannelId = playoffChannelId;
		this.gdcMode = gdcMode;
		this.playoffMode = playoffMode;
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
		GDCMode gdcMode = GDCMode.parse(doc.getInteger("gdcMode", 0));
		PlayoffMode playoffMode = PlayoffMode.parse(doc.getInteger("playoffMode", 0));

		if (gdcMode == null) {
			gdcMode = useThreads ? GDCMode.SING_CHNL_W_THRD : GDCMode.SING_CHNL_NO_THRD;
		}

		if (playoffMode == null) {
			playoffMode = PlayoffMode.OFF;
		}

		return new GuildPreferences(teams, gdcChannelId, playoffChannelId, gdcMode, playoffMode);
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

	public Long getGameDayChannelId() {
		return gdcChannelId;
	}

	public void setGameDayChannelId(Long channelId) {
		this.gdcChannelId = channelId;
	}

	public void setPlayoffChannelId(Long channelId) {
		this.playoffChannelId = channelId;
	}

	public GDCMode getGDCMode() {
		return gdcMode;
	}

	public void setGDCMode(GDCMode mode) {
		this.gdcMode = mode;
	}

	public PlayoffMode getPlayoffMode() {
		return playoffMode;
	}

	public void setPlayoffMode(PlayoffMode mode) {
		this.playoffMode = mode;
	}

	public Long getPlayoffChannelId() {
		return playoffChannelId;
	}

	@Override
	public String toString() {
		return "GuildPreferences [teams=" + teams + ", gdcChannelId=" + gdcChannelId + ", playoffChannelId="
			+ playoffChannelId + ", gdcMode=" + gdcMode + ", playoffMode=" + playoffMode + "]";
	}

}
