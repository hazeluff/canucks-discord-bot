package com.hazeluff.discord.utils;

import com.hazeluff.discord.ahl.AHLTeams.Team;

public class HazeluffUtils {

	public static void main(String[] argv) {
		com.hazeluff.discord.ahl.AHLGameScheduler ahlGameScheduler = new com.hazeluff.discord.ahl.AHLGameScheduler();
		ahlGameScheduler.start();

		while (!ahlGameScheduler.isInit()) {
			if (!ahlGameScheduler.isInit()) {
				System.out.println("Waiting for AHL GameScheduler...");
			}
			Utils.sleep(10000);
		}

		System.out.println("Act## " + ahlGameScheduler.getActivePlayoffGames(Team.ABBY_NUCKS));
		System.out.println("");
		System.out.println("Cur## " + ahlGameScheduler.getCurrentLivePlayoffGame(Team.ABBY_NUCKS));
		System.out.println("");
		System.out.println("Fut## " + ahlGameScheduler.getFuturePlayoffGames(Team.ABBY_NUCKS));
		System.out.println("");
		System.out.println("Past## " + ahlGameScheduler.getPastPlayoffGames(Team.ABBY_NUCKS));
		ahlGameScheduler.interrupt();
	}
}
