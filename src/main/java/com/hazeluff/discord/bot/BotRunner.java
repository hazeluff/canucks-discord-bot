package com.hazeluff.discord.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.ahl.AHLGameScheduler;
import com.hazeluff.discord.nhl.NHLGameScheduler;
import com.hazeluff.discord.nhl.NHLPlayoffBracketFetcher;
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
			NHLBot nhlbot = NHLBot.create(args[0], null, null, null);
			nhlbot.deployScript();
			return;
		}


		/*
		 * Regular Start
		 */
		NHLGameScheduler nhlGameScheduler = new NHLGameScheduler();
		AHLGameScheduler ahlGameScheduler = new AHLGameScheduler();
		NHLPlayoffBracketFetcher nhlPlayoffBracketFetcher = new NHLPlayoffBracketFetcher();

		if (Config.Debug.isLoadGames()) {
			LOGGER.info("Starting GameSchedulers");
			nhlGameScheduler.start();
			ahlGameScheduler.start();
			nhlPlayoffBracketFetcher.start();
		} else {
			nhlGameScheduler.setInit(true);
			ahlGameScheduler.setInit(true);
			nhlPlayoffBracketFetcher.setInit(true);
		}

		while (!nhlGameScheduler.isInit() && !ahlGameScheduler.isInit() && !ahlGameScheduler.isInit()) {
			LOGGER.info(
				"Waiting for Initialization - "
					+ "NHLGameScheduler={}, AHLGameScheduler={}, NHLPlayoffSeriesFetcher={}",
				nhlGameScheduler.isInit() ? "Done" : "Initializing..",
				ahlGameScheduler.isInit() ? "Done" : "Initializing..",
				nhlPlayoffBracketFetcher.isInit() ? "Done" : "Initializing..");

			Utils.sleep(10000);
		}
		LOGGER.info("GameSchedulers are initialized!");

		NHLBot.create(args[0], nhlGameScheduler, ahlGameScheduler, nhlPlayoffBracketFetcher).start();
    }
}
