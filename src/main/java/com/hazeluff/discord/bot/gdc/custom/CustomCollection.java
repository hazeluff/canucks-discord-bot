package com.hazeluff.discord.bot.gdc.custom;

import static com.hazeluff.discord.bot.gdc.custom.CustomMessage.team;

import com.hazeluff.nhl.Team;

@SuppressWarnings("serial")
public class CustomCollection extends CustomMessageCollection {
	public CustomCollection() {
		super();
		// Canucks
		add(team("https://www.youtube.com/watch?v=a3MAg_0ctBs", Team.VANCOUVER_CANUCKS));
	}
}
