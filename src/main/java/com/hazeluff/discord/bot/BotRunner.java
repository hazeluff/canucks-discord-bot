package com.hazeluff.discord.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.utils.Utils;

public class BotRunner {
	private static final Logger LOGGER = LoggerFactory.getLogger(BotRunner.class);

	public static void main(String[] args) {
		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());

		/*
		 * Script Only Deploy/Start
		 */
		if (Config.Deploy.isScriptOnly()) {
			// Do not #start bot.
			// Without #start the bot will only have Discord Client abilities.
			NHLBot nhlbot = NHLBot.create(null, null, args[0]);
			nhlbot.deployScript();
			return;
		}


		/*
		 * Regular Start
		 */
		com.hazeluff.discord.nhl.NHLGameScheduler nhlGameScheduler = new com.hazeluff.discord.nhl.NHLGameScheduler();
		com.hazeluff.discord.ahl.AHLGameScheduler ahlGameScheduler = new com.hazeluff.discord.ahl.AHLGameScheduler();

		if (Config.Debug.isLoadGames()) {
			LOGGER.info("Loading the games...");
			nhlGameScheduler.start();
			ahlGameScheduler.start();
		} else {
			nhlGameScheduler.setInit(true);
			ahlGameScheduler.setInit(true);
		}

		while (!nhlGameScheduler.isInit() && !ahlGameScheduler.isInit()) {
			if (!nhlGameScheduler.isInit()) {
				LOGGER.info("Waiting for NHL GameScheduler...");
			}
			if (!ahlGameScheduler.isInit()) {
				LOGGER.info("Waiting for AHL GameScheduler...");
			}
			Utils.sleep(10000);
		}

		NHLBot.create(nhlGameScheduler, ahlGameScheduler, args[0]).start();
    }
}
