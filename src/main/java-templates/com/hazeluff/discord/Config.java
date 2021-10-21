package com.hazeluff.discord;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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

	private static final Properties systemProperties = System.getProperties();

	/* 
	 * Quick Config
	 */
	public static final Season CURRENT_SEASON = Seasons.S21_22;

	// List of guilds allowed to access the bot. (not strictly enforced access)
	public static final List<Long> DEV_GUILD_LIST = Arrays.asList(
			268247727400419329l, 
			276953120964083713l
	);

	// List of guilds allowed to access the bot. (not strictly enforced access)
	public static final List<Long> SERVICED_GUILD_LIST = Arrays.asList( 
			238870084003561472l // /r/canucks
	);
	
	/*
	 *  Technical Config
	 */
	public static final int HTTP_REQUEST_RETRIES = 5;
	public static final String NHL_API_URL = "https://statsapi.web.nhl.com/api/v1";	
	
	/*
	 *  About
	 */
	public static final String APPLICATION_NAME = "${project.name}";
	public static final String GIT_URL = "http://canucks-discord.hazeluff.com/";
	
	public static final String DONATION_URL = "https://paypal.me/hazeluff";
	public static final String DONATION_DOGE = "DK58dzmNCExxCocq9tMbYVzg3rdWuYsbY8";
	public static final String DONATION_ETH = "0x313faE0D36BFf3F7a4817E52a71B74e2924D4b97";
	public static final long HAZELUFF_ID = 225742618422673409l;
	public static final String HAZELUFF_MENTION = "<@225742618422673409>";
	public static final String HAZELUFF_SITE = "http://www.hazeluff.com";
	public static final String HAZELUFF_EMAIL = "me@hazeluff.com";
	public static final String HAZELUFF_TWITTER = "@Hazeluff";
	
	public static final String VERSION = "${project.version}";
	
	public static final String MONGO_HOST = "localhost";
	public static final int MONGO_PORT = 27017;
	public static final String MONGO_DATABASE_NAME = "NHLBot";
	public static final String MONGO_TEST_DATABASE_NAME = "NHLBotIntegrationTest";
	public static final ZoneId DATE_START_TIME_ZONE = ZoneId.of("America/Vancouver");
	
	public static final String STATUS_MESSAGE = "/help for commands";
}
