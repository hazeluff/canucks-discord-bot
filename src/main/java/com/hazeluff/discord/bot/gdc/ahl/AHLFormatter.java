package com.hazeluff.discord.bot.gdc.ahl;

import com.hazeluff.ahl.game.AHLGame;

public class AHLFormatter {
	public static String getMatchup(AHLGame game) {
		return String.format(
			"**%s** vs **%s**", 
			game.getHomeTeam().getLocationName(), game.getAwayTeam().getLocationName());
	}
}
