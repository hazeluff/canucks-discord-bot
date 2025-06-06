package com.hazeluff.discord.bot.gdc.nhl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.channel.GDCCategoryManager;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.nhl.NHLGameTracker;
import com.hazeluff.discord.nhl.NHLTeams.Team;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.game.Game;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;

/**
 * This class is used to manage the channels in a Guild.
 */
public class NHLGameDayChannelsManager extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(NHLGameDayChannelsManager.class);

	// Poll for every 5 seconds, (On initialization)
	static final long INIT_UPDATE_RATE = 5000L;
	// Poll for every 5 minutes - if the scheduler has updated
	static final long UPDATE_RATE = 300000L;

	private final NHLBot nhlBot;
	// Map<GuildId, Map<GamePk, GameDayChannel>>
	private final Map<Long, Map<Integer, NHLGameDayChannelThread>> gameDayChannels;

	Map<Long, Map<Integer, NHLGameDayChannelThread>> getGameDayChannels() {
		return new ConcurrentHashMap<>(gameDayChannels);
	}

	public NHLGameDayChannelThread getGameDayChannel(long guildId, int gamePk) {
		if (!gameDayChannels.containsKey(guildId)) {
			return null;
		}

		if (!gameDayChannels.get(guildId).containsKey(gamePk)) {
			return null;
		}

		return gameDayChannels.get(guildId).get(gamePk);
	}

	Map<Integer, NHLGameDayChannelThread> getGameDayChannels(long guildId) {
		if (!gameDayChannels.containsKey(guildId)) {
			return Collections.emptyMap();
		}
		return gameDayChannels.get(guildId);
	}

	boolean isGameDayChannelExist(long guildId, int gamePk) {
		return getGameDayChannel(guildId, gamePk) != null;
	}

	void addGameDayChannel(long guildId, int gamePk, NHLGameDayChannelThread gameDayChannel) {
		if (!gameDayChannels.containsKey(guildId)) {
			gameDayChannels.put(guildId, new ConcurrentHashMap<>());
		}
		gameDayChannels.get(guildId).put(gamePk, gameDayChannel);
	}

	/**
	 * 
	 * @param guildId
	 * @param gamePk
	 * @return the removed GameDayChannel
	 */
	NHLGameDayChannelThread removeGameDayChannel(long guildId, int gamePk) {
		if (!gameDayChannels.containsKey(guildId)) {
			return null;
		}

		Map<Integer, NHLGameDayChannelThread> guildChannels = gameDayChannels.get(guildId);
		NHLGameDayChannelThread gameDayChannel = guildChannels.remove(gamePk);
		if (gameDayChannel == null) {
			return null;
		}

		gameDayChannel.stopAndRemoveGuildChannel();

		if (guildChannels.isEmpty()) {
			gameDayChannels.remove(guildId);
		}
		return gameDayChannel;
	}

	public NHLGameDayChannelsManager(NHLBot nhlBot) {
		this.nhlBot = nhlBot;
		gameDayChannels = new ConcurrentHashMap<>();
	}

	@Override
	public void run() {
		LOGGER.info("GameDayChannelsManager Thread started.");
		LocalDate lastUpdate = null;
		while (!isStop()) {
			try {
				LocalDate schedulerUpdate = nhlBot.getNHLGameScheduler().getLastUpdate();
				if (schedulerUpdate == null) {
					LOGGER.info("Waiting for GameScheduler to initialize...");
					Utils.sleep(INIT_UPDATE_RATE);
				} else if (lastUpdate == null || schedulerUpdate.compareTo(lastUpdate) > 0) {
					LOGGER.info("Updating Channels...");
					updateChannels();
					lastUpdate = schedulerUpdate;
				} else {
					LOGGER.debug("Waiting for GameScheduler to update...");
					Utils.sleep(UPDATE_RATE);
				}
			} catch (Exception e) {
				LOGGER.error("Error occured when updating channels.", e);
			}
		}
	}

	/**
	 * Gets the guilds that are subscribed to the specified team.
	 * 
	 * @param team
	 *            team that the guilds are subscribed to
	 * @return list of IGuilds
	 */
	public List<Guild> getSubscribedGuilds(Team team) {
		return nhlBot.getDiscordManager().getGuilds().stream().filter(guild -> {
			long guildId = guild.getId().asLong();
			return nhlBot.getPersistentData()
					.getPreferencesData()
					.getGuildPreferences(guildId)
					.getTeams()
					.contains(team);
		}).collect(Collectors.toList());
	}

	/**
	 * Creates a GameDayChannel for the given game-guild pair. If the channel exist,
	 * the existing one will be returned.
	 * 
	 * @param game
	 * @param guild
	 */
	public NHLGameDayChannelThread createChannel(Game game, Guild guild) {
		LOGGER.info("Initializing for channel. channelName={}, guild={}",
				NHLGameDayChannelsManager.buildChannelName(game), guild.getName());
		int gamePk = game.getGameId();
		long guildId = guild.getId().asLong();

		NHLGameDayChannelThread gameDayChannel = getGameDayChannel(guildId, gamePk);
		if (gameDayChannel == null) {
			NHLGameTracker gameTracker = nhlBot.getNHLGameScheduler().getGameTracker(game);
			if (gameTracker != null) {
				gameDayChannel = createGameDayChannel(nhlBot, gameTracker, guild);
			} else {
				LOGGER.error("Could not find GameTracker for game [{}]", game);
			}
		} else {
			LOGGER.debug("Game Day Channel already exists for game [{}] in guild [{}]", gamePk, guildId);
		}

		return gameDayChannel;
	}

	NHLGameDayChannelThread createGameDayChannel(NHLBot nhlBot, NHLGameTracker gameTracker, Guild guild) {
		LOGGER.info("Creating channel. channelName={}, guild={}",
				NHLGameDayChannelsManager.buildChannelName(gameTracker.getGame()), guild.getName());
		NHLGameDayChannelThread channel = NHLGameDayChannelThread.get(nhlBot, gameTracker, guild);
		addGameDayChannel(guild.getId().asLong(), gameTracker.getGame().getGameId(), channel);
		return channel;
	}

	/**
	 * Initializes the channels of guild in Discord. Creates channels for the latest
	 * games of the current team the guild is subscribed to.
	 * 
	 * @param guild
	 *            guild to initialize channels for
	 */
	void updateChannels() {
		LOGGER.info("Updating channels for all guilds.");
		List<Guild> guilds = nhlBot.getDiscordManager().getGuilds().stream()
				.filter(guild -> !nhlBot.getPersistentData().getPreferencesData()
						.getGuildPreferences(guild.getId().asLong())
						.getTeams()
						.isEmpty()
				)
				.collect(Collectors.toList());

		// Update Dev Guilds
		List<Guild> devGuilds = guilds.stream()
				.filter(guild -> Config.DEV_GUILD_LIST.contains(guild.getId().asLong()))
				.collect(Collectors.toList());

		devGuilds.stream().forEach(devGuild -> {
			LOGGER.info("Updating channels of dev guilds: " + devGuild.getId().asLong());
			updateChannels(devGuild);
		});

		// Update Other Guilds
		List<Guild> otherGuilds = guilds.stream()
				.filter(guild -> !Config.DEV_GUILD_LIST.contains(guild.getId().asLong()))
				.collect(Collectors.toList());

		otherGuilds.stream().forEach(guild -> {
			LOGGER.info("Updating channels of other guilds: " + guild.getId().asLong());
			updateChannels(guild);
			Utils.sleep(500);
		});
	}

	/**
	 * <p>
	 * Creates and deletes channels, to match the current subscribed teams of a
	 * guild.
	 * </p>
	 * 
	 * <p>
	 * Removes the GameDayChannels from the stored Map
	 * </p>
	 * 
	 * @param guild
	 */
	public void updateChannels(Guild guild) {
		try {
			GuildPreferences preferences = nhlBot.getPersistentData().getPreferencesData()
					.getGuildPreferences(guild.getId().asLong());
			List<Team> teams = nhlBot.getPersistentData().getPreferencesData()
					.getGuildPreferences(guild.getId().asLong())
					.getTeams();

			LOGGER.info("Updating Channels for [{}]: activeGames={}",
					guild.getId().asLong(),
					nhlBot.getNHLGameScheduler().getActiveGames(teams).stream()
							.map(NHLGameDayChannelsManager::buildChannelName)
							.collect(Collectors.toList()));

			// Remove channels of outdated/unsubscribed games
			for (TextChannel channel : DiscordManager.getTextChannels(guild)) {
				if (isRemoveChannel(channel, preferences)) {
					deleteChannel(channel, preferences);
				}
			}

			for (Entry<Integer, NHLGameDayChannelThread> gdcEntry : getGameDayChannels(guild.getId().asLong()).entrySet()) {
				NHLGameDayChannelThread gdc = gdcEntry.getValue();
				gdc.updateIntroMessage();
			}

			// Create game channels of latest game for current subscribed team
			for (Game game : nhlBot.getNHLGameScheduler().getActiveGames(teams)) {
				createChannel(game, guild);
			}
		} catch (Exception e) {
			LOGGER.warn("Issue updating guild: " + guild.getId().asLong() + "\n" + e.getMessage());
		}
	}

	/**
	 * Deletes a given channel from its guild.
	 * 
	 * @param channel
	 *            channel to remove
	 * @param preferences
	 *            the preferences of the guild of the channel. This is used to
	 *            determine if a game is active for the guild.
	 */
	/*
	 * GuildPreferences is passed in so as to not fetch it each channel in a loop in
	 * #deleteInactiveGuildChannels(IGuild).
	 */
	void deleteChannel(TextChannel channel, GuildPreferences preferences) {
		LOGGER.info("Remove channel: " + channel.getName());
		/*
		 * Remove games that:
		 * - are in the category
		 * - have the correct name format for a gdc
		 * - are not active
		 */
		Game game = nhlBot.getNHLGameScheduler().getGameByChannelName(channel.getName());
		if (game != null) {
			NHLGameDayChannelThread removedChannel = removeGameDayChannel(channel.getGuildId().asLong(), game.getGameId());
			if (removedChannel == null) {
				DiscordManager.deleteChannel(channel);
			}
		}
	}

	boolean isRemoveChannel(TextChannel channel, GuildPreferences preferences) {
		// Does not remove channels not in the Game Day Channel Category
		if (!isInGameDayCategory(channel)) {
			return false;
		}

		// Does not remove channels that does not have the correct name format
		if (!isChannelNameFormat(channel.getName())) {
			return false;
		}

		// Does not remove active games
		if (isGameActive(preferences.getTeams(), channel.getName())) {
			return false;
		}

		return true;
	}

	public boolean isInGameDayCategory(TextChannel channel) {
		Category category = DiscordManager.getCategory(channel);
		return category == null ? false : category.getName().equalsIgnoreCase(GDCCategoryManager.CATEGORY_NAME);
	}

	/**
	 * Determines if the given channel name is that of a possible game. Does not
	 * factor into account whether or not the game is real.
	 * 
	 * @param channelName
	 *            name of the channel
	 * @return true, if is of game channel format;<br>
	 *         false, otherwise.
	 */
	public static boolean isChannelNameFormat(String channelName) {
		String teamRegex = String.join("|", Arrays.asList(Team.values()).stream()
				.map(team -> team.getCode().toLowerCase()).collect(Collectors.toList()));
		teamRegex = String.format("(%s)", teamRegex);
		String year = String.valueOf(Config.NHL_CURRENT_SEASON.getEndYear()).substring(2, 4);
		String regex = String.format("%1$s-vs-%1$s-%2$s-[0-9]{2}-[0-9]{2}", teamRegex, year);
		return channelName.matches(regex);
	}

	/**
	 * Gets the name that a channel in Discord related to this game would have.
	 * 
	 * @param game
	 *            game to get channel name for
	 * @return channel name in format: "AAA-vs-BBB-yy-MM-DD". <br>
	 *         AAA is the 3 letter code of home team<br>
	 *         BBB is the 3 letter code of away team<br>
	 *         yy-MM-DD is a date format
	 */
	public static String buildChannelName(Game game) {
		String channelName = String.format("%.3s-vs-%.3s-%s", game.getHomeTeam().getCode(),
				game.getAwayTeam().getCode(), buildChannelDate(game));
		return channelName.toLowerCase();

	}

	/**
	 * Gets the date in the format "yy-MM-dd"
	 * 
	 * @param game
	 *            game to get the date from
	 * @param zone
	 *            time zone to convert the time to
	 * @return the date in the format "yy-MM-dd"
	 */
	public static String buildChannelDate(Game game) {
		return game.getStartTime().format(DateTimeFormatter.ofPattern("yy-MM-dd"));
	}

	boolean isGameActive(List<Team> teams, String channelName) {
		return nhlBot.getNHLGameScheduler().isGameActive(teams, channelName);
	}

	/**
	 * Used for stubbing the loop of {@link #run()} for tests.
	 * 
	 * @return
	 */
	boolean isStop() {
		return false;
	}
}
