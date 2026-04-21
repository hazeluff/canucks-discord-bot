package com.hazeluff.discord.bot.gdc.nhl;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.hazeluff.nhl.game.NHLGame;

public class NHLFormatter {
	public static String getMatchup(NHLGame game) {
		return String.format(
			"**%s** vs **%s**", 
			game.getHomeTeam().getLocationName(), game.getAwayTeam().getLocationName());
	}

	public static String getThreadDate(NHLGame game) {
		return game.getStartTime().format(DateTimeFormatter.ofPattern("MMM d yyyy"));
	}

	/**
	 * Gets the date in the format "EEEE dd MMM yyyy"
	 * 
	 * @param game
	 *            game to get the date for
	 * @param zone
	 *            time zone to convert the time to
	 * @return the date in the format "EEEE dd MMM yyyy"
	 */
	public static String getNiceDate(NHLGame game, ZoneId zone) {
		return game.getStartTime().withZoneSameInstant(zone).format(DateTimeFormatter.ofPattern("EEEE, d/MMM/yyyy"));
	}

	public static String getStartTime(NHLGame game, ZoneId zone) {
		return game.getStartTime().withZoneSameInstant(zone).format(DateTimeFormatter.ofPattern("H:mm z"));
	}
}
