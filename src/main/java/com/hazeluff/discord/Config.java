package com.hazeluff.discord;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.hazeluff.discord.bot.command.AboutCommand;
import com.hazeluff.discord.bot.command.GDCCommand;
import com.hazeluff.discord.bot.command.HelpCommand;
import com.hazeluff.discord.bot.command.NHLStatsCommand;
import com.hazeluff.discord.bot.command.NextGameCommand;
import com.hazeluff.discord.bot.command.ScheduleCommand;
import com.hazeluff.discord.bot.command.BotStatsCommand;
import com.hazeluff.discord.bot.command.SubscribeCommand;
import com.hazeluff.discord.bot.command.ThreadsCommand;
import com.hazeluff.discord.bot.command.UnsubscribeCommand;
import com.hazeluff.discord.nhl.Seasons;
import com.hazeluff.discord.nhl.Seasons.Season;

public class Config {
	public static class Debug {
		private static final String LOAD_GAMES_KEY = "load.games";

		public static boolean isLoadGames() {
			boolean hasKey = systemProperties.containsKey(LOAD_GAMES_KEY);
			if(!hasKey) {
				return true;
			}
			String strValue = systemProperties.getProperty(LOAD_GAMES_KEY);
			return strValue.isEmpty() || Boolean.valueOf(strValue);
		}
	}
	
	public static class Deploy {
		private static final String SCRIPT_ONLY_KEY = "deploy.scripts";

		public static boolean isScriptOnly() {
			boolean hasKey = systemProperties.containsKey(SCRIPT_ONLY_KEY);
			if(!hasKey) {
				return false;
			}
			String strValue = systemProperties.getProperty(SCRIPT_ONLY_KEY);
			return strValue.isEmpty() || Boolean.valueOf(strValue);
		}
	}

	private static final Properties systemProperties = System.getProperties();

	/* 
	 * Season Config
	 */
	public static final Season CURRENT_SEASON = Seasons.S23_24;

	// List of guilds allowed to access the bot. (not strictly enforced access)
	public static final List<Long> DEV_GUILD_LIST = Arrays.asList(
			268247727400419329l, 
			276953120964083713l
	);
	
	/*
	 *  Technical Config
	 */
	public static final int HTTP_REQUEST_RETRIES = 5;
	public static final String NHL_API_URL = "https://api-web.nhle.com/v1";	
	
	/*
	 *  About
	 */
	public static final String APPLICATION_NAME = ProjectInfo.APPLICATION_NAME;
	public static final String GIT_URL = "http://nhlbot.hazeluff.com/";
	
	public static final String DONATION_URL = "https://paypal.me/hazeluff";
	public static final String DONATION_DOGE = "DK58dzmNCExxCocq9tMbYVzg3rdWuYsbY8";
	public static final String DONATION_ETH = "hazeluff.eth (0x313faE0D36BFf3F7a4817E52a71B74e2924D4b97)";
	public static final long HAZELUFF_ID = 225742618422673409l;
	public static final String HAZELUFF_MENTION = "<@225742618422673409>";
	public static final String HAZELUFF_SITE = "http://www.hazeluff.com";
	public static final String HAZELUFF_EMAIL = "me@hazeluff.com";
	public static final String HAZELUFF_TWITTER = "@Hazeluff";
	
	public static final String VERSION = ProjectInfo.VERSION;

	
	private static final String MONGO_HOST_KEY = "mongo.host";
	public static final String MONGO_HOST_DEFAULT = "localhost";
	public static final int MONGO_PORT = 27017;
	public static String getMongoHost() {
		boolean hasKey = systemProperties.containsKey(MONGO_HOST_KEY);
		if(!hasKey) {
			return MONGO_HOST_DEFAULT;
		}
		return systemProperties.getProperty(MONGO_HOST_KEY);
	}
	
	public static final String MONGO_DATABASE_NAME = "NHLBot";
	public static final String MONGO_TEST_DATABASE_NAME = "NHLBotIntegrationTest";
	public static final ZoneId DATE_START_TIME_ZONE = ZoneId.of("America/Vancouver");
	
	public static final String STATUS_MESSAGE = "/help for commands";
	
	// Slash Commands
	/**
	 * <p>
	 * Configures which Commands are used/added to discord as slash commands.
	 * </p>
	 * 
	 * <p>
	 * NEW COMMANDS NEED TO BE ADDED HERE!
	 * </p>
	 * 
	 * @param nhlBot
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static List<Class> getSlashCommands() {
		return Arrays.asList(
				AboutCommand.class,
				NHLStatsCommand.class,
				GDCCommand.class,
				HelpCommand.class,
				NextGameCommand.class,
				SubscribeCommand.class,
				ScheduleCommand.class,
				BotStatsCommand.class,
				ThreadsCommand.class,
				UnsubscribeCommand.class
		);
	}
}
