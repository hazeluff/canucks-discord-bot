package com.hazeluff.discord.bot.gdc.nhl.custom.game;

import com.hazeluff.discord.nhl.NHLTeams.Team;

@SuppressWarnings("serial")
public class CanucksStartGameCollection extends CustomGameMessage.Collection {
	Team getTeam() {
		return Team.VANCOUVER_CANUCKS;
	}

	public CanucksStartGameCollection() {
		super();

		any("");
	}
}
