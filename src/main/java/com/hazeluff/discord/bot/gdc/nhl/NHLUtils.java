package com.hazeluff.discord.bot.gdc.nhl;

import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.game.NHLGame;
import com.hazeluff.nhl.game.PeriodType;

public class NHLUtils {
	public static String toNiceStatus(NHLGame game) {
		if (!game.getGameState().isStarted())
			return String.format("Not Started (%s)", game.getGameState());
		if (game.getGameState().isStarted())
			return String.format("Finished (%s)", game.getGameState());
		
		int period = game.getPeriodNumber();
		PeriodType type = game.getPeriodType();
		int otPeriod = period - game.getMaxRegularPeriods();

		String typeStr;
		switch (type) {
		case SHOOTOUT:
			typeStr = type.getNiceName();
			break;
		case OVERTIME:
			if (otPeriod > 0)
				typeStr = "OT" + otPeriod;
			else
				typeStr = type.getNiceName();
			break;
		case REGULAR:
		default:
			typeStr = period + Utils.getOrdinal(period);
			break;
		}

		if(game.isInIntermission())
			return String.format("Intermission (%s)", typeStr);
		else if (type == PeriodType.REGULAR)
			return String.format("Regulation: %s Period", typeStr);
		else
			return String.format("Period: %s", typeStr);
	}
}
