package com.hazeluff.discord;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.hazeluff.ahl.AHLGateway;
import com.hazeluff.discord.ahl.AHLSeasons;
import com.hazeluff.discord.bot.command.AboutCommand;
import com.hazeluff.discord.bot.command.ConfigCommand;
import com.hazeluff.discord.bot.command.ConfigPlayoffCommand;
import com.hazeluff.discord.bot.command.GDCCommand;
import com.hazeluff.discord.bot.command.HelpCommand;
import com.hazeluff.discord.bot.command.NHLStatsCommand;
import com.hazeluff.discord.bot.command.NextGameCommand;
import com.hazeluff.discord.bot.command.ScheduleCommand;
import com.hazeluff.discord.bot.command.SubscribeCommand;
import com.hazeluff.discord.bot.command.UnsubscribeCommand;
import com.hazeluff.discord.nhl.NHLSeasons;
import com.hazeluff.discord.nhl.NHLSeasons.Season;
import com.hazeluff.discord.nhl.NHLTeams.Team;

import discord4j.core.object.entity.Guild;

public class Config {
	public static class Debug {
		private static final String LOAD_GAMES_KEY = "load.games";

		public static boolean isLoadGames() {
			return loadBool(LOAD_GAMES_KEY, true);
		}

		private static final String DEBUG_AS_NON_DEV = "debug.nondev";

		public static boolean isDebugAsNonDev() {
			return loadBool(DEBUG_AS_NON_DEV, false);
		}

		private static final String DEBUG_FINISHED_GAMES = "debug.finishedgames";

		public static boolean isAllowFinishedGames() {
			return loadBool(DEBUG_FINISHED_GAMES, false);
		}
	}
	
	public static class Deploy {
		private static final String SCRIPT_ONLY_KEY = "deploy.scripts";

		public static boolean isScriptOnly() {
			return loadBool(SCRIPT_ONLY_KEY, false);
		}
	}

	private static final Properties systemProperties = System.getProperties();

	/*
	 * Guild/Server lists.
	 */
	// List of guilds allowed to access the bot. (not strictly enforced access)
	public static final List<Long> DEV_GUILD_LIST = Arrays.asList(
		268247727400419329l,
		276953120964083713l
	);
	
	public static boolean isDevGuild(Guild guild) {
		return DEV_GUILD_LIST.contains(guild.getId().asLong());
	}

	public static boolean isDevGuild(long guildId) {
		return DEV_GUILD_LIST.contains(guildId);
	}

	/*
	 * NHL Config
	 */
	public static final Season NHL_CURRENT_SEASON = NHLSeasons.S25_26;

	public static final Team DEFAULT_TEAM = Team.VANCOUVER_CANUCKS;
	public static final String NHL_API_URL = "https://api-web.nhle.com/v1";

	/*
	 * AHL Config
	 */
	public static final AHLSeasons.Season AHL_CURRENT_SEASON = AHLSeasons.S25_26;
	public static final Team AHL_DEFAULT_TEAM = Team.VANCOUVER_CANUCKS;
	public static final String AHL_API_CONFIG_URL = 
			"https://lscluster.hockeytech.com/statview-1.4.1/js/client/ahl/base.r3.js"; // Used to fetch client key.
	public static final String AHL_API_KEY;
	public static final String AHL_API_CLIENT_CODE;
	static {
		String[] config = AHLGateway.getConfig();
		AHL_API_KEY = config[0];
		AHL_API_CLIENT_CODE = config[1];
		if (AHL_API_KEY == null || AHL_API_CLIENT_CODE == null) {
			throw new NullPointerException(
					"AHL API Config returned null value(s):" + AHL_API_KEY + "," + AHL_API_CLIENT_CODE);
		}
	}
	public static final String AHL_API_URL = "https://lscluster.hockeytech.com";
	
	/*
	 *  Technical Config
	 */
	public static final ZoneId ZONE_ID = ZoneId.of("America/Toronto");
	public static final int HTTP_REQUEST_RETRIES = 1;
	public static final long GUILD_ID = 238870084003561472l;
	
	/*
	 *  About
	 */
	public static final List<String> STATUS_MESSAGES = Arrays.asList(
		"/help for commands",
		"Слава Україні!",
		"Slava Ukraini",
		"光復香港 時代革命",
		"香港人加油",
		"Taiwan #1",
		"Elbows Up",
		"Je suis Canadien",
		"\"So let us be loving, hopeful and optimistic. And we'll change the world.\"", // Jack Layton
		"Free Palestine",
		"Stay safe, stay legal.",
		"Fuck messier",
		"Fuck the oilers",
		"Fuck the leafs",
		"I am not a cat",
		"Can't stop, won't stop",
		"To the moon!",
		"So fake. No room.",
		"\"Follow your dreams. Listen to your heart. Obey your passion.\"", // Pat Quinn
		"Watching: Lucky Star",	
		"Watching: The Lego Movie (2014)",
		"Watching: Mewtwo Strikes Back (1999)",
		"Playing: ULTRAKILL",
		"Listening: Linkin Park - Meteora (2003)",
		"No items, Fox only, Final Destination",
		"There's a creeper on the roof. LA-LA-LA-LA-LA",
		"It was 4-1"
	);

	public static final String APPLICATION_NAME = ProjectInfo.APPLICATION_NAME;
	public static final String GIT_URL = "https://github.com/hazeluff/canucks-discord-bot/tree/nhlbot";
	
	public static final String DONATION_URL = "https://paypal.me/hazeluff";
	public static final String DONATION_ETH = "hazeluff.eth (0x313faE0D36BFf3F7a4817E52a71B74e2924D4b97)";
	public static final String DONATION_MATIC = "0x313faE0D36BFf3F7a4817E52a71B74e2924D4b97";
	public static final long HAZELUFF_ID = 225742618422673409l;
	public static final String HAZELUFF_MENTION = "<@225742618422673409>";
	public static final String HAZELUFF_SITE = "http://www.hazeluff.com";
	public static final String HAZELUFF_EMAIL = "me@hazeluff.com";
	public static final String HAZELUFF_TWITTER = "@Hazeluff";

	public static final String VERSION = ProjectInfo.VERSION;

	private static final String MONGO_HOST_KEY = "mongo.host";
	private static final String MONGO_HOST_DEFAULT = "localhost";
	public static String getMongoHost() {
		return loadString(MONGO_HOST_KEY, MONGO_HOST_DEFAULT);
	}

	private static final String MONGO_PORT_KEY = "mongo.port";
	private static final int MONGO_PORT_DEFAULT = 27017;

	public static int getMongoPort() {
		return loadInt(MONGO_PORT_KEY, MONGO_PORT_DEFAULT);
	}

	private static final String MONGO_USER_KEY = "mongo.username";

	public static String getMongoUserName() {
		return loadString(MONGO_USER_KEY, null);
	}

	private static final String MONGO_PASS_KEY = "mongo.password";

	public static String getMongoPassword() {
		return loadString(MONGO_PASS_KEY, null);
	}
	
	public static final String MONGO_DATABASE_NAME = "NHLBot";
	public static final String MONGO_TEST_DATABASE_NAME = "NHLBotIntegrationTest";
	public static final ZoneId SERVER_ZONE = ZoneId.of("America/Vancouver");

	// Slash Commands
	@SuppressWarnings("rawtypes")
	public final static List<Class> SLASH_COMMANDS = Arrays.asList(
		AboutCommand.class,
		ConfigCommand.class,
		ConfigPlayoffCommand.class,
		NHLStatsCommand.class,
		GDCCommand.class,
		HelpCommand.class,
		NextGameCommand.class,
		SubscribeCommand.class,
		ScheduleCommand.class,
		UnsubscribeCommand.class
	);

	// Utils
	private static boolean loadBool(String key, boolean def) {
		boolean hasKey = systemProperties.containsKey(key);
		if (!hasKey) {
			return def;
		}
		String strValue = systemProperties.getProperty(key);
		return strValue.isEmpty() || Boolean.valueOf(strValue);
	}

	private static int loadInt(String key, int def) {
		boolean hasKey = systemProperties.containsKey(key);
		if (!hasKey) {
			return def;
		}
		return Integer.parseInt(systemProperties.getProperty(key));
	}

	private static String loadString(String key, String def) {

		boolean hasKey = systemProperties.containsKey(key);
		if (!hasKey) {
			return def;
		}
		return systemProperties.getProperty(key);
	}
}
