package com.hazeluff.discord.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.nhl.GameScheduler;
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
			NHLBot nhlbot = NHLBot.create(null, args[0]);
			nhlbot.deployScript();
			return;
		}


		/*
		 * Regular Start
		 */
		GameScheduler gameScheduler = new GameScheduler();

		if (Config.Debug.isLoadGames()) {
			LOGGER.info("Loading the games...");
			gameScheduler.start();
		} else {
			gameScheduler.setInit(true);
		}

		while (!gameScheduler.isInit()) {
			LOGGER.info("Waiting for GameScheduler...");
			Utils.sleep(2000);
		}

		NHLBot.create(gameScheduler, args[0]).start();
    }
}
