package com.hazeluff.discord.utils;

public class HazeluffUtils {

	public static void main(String[] argv) {
		com.hazeluff.discord.ahl.AHLGameScheduler ahlGameScheduler = new com.hazeluff.discord.ahl.AHLGameScheduler();
		ahlGameScheduler.start();
		while (!ahlGameScheduler.isInit()) {
			System.out.println("Waiting for AHL GameScheduler...");
			Utils.sleep(10000);
		}
		System.out.println(ahlGameScheduler.getGames().size());
	}
}
