package com.hazeluff.discord.bot.gdc.nhl;

import java.time.LocalDate;
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
import com.hazeluff.discord.utils.InterruptableThread;
import com.hazeluff.nhl.game.Game;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;

/**
 * This class is used to manage creating GDCs for a guild.<br/>
 * Channels are created for each "active" game the guild is subscribed to.
 */
public class NHLGdcGuildManager extends InterruptableThread {
	private static final Logger LOGGER = LoggerFactory.getLogger(NHLGdcGuildManager.class);

	private final static Map<Long, NHLGdcGuildManager> managers = new ConcurrentHashMap<>();

	// Poll for every 5 seconds, (On initialization)
	static final long INIT_UPDATE_RATE = 5000L;
	// Poll for every 5 minutes - if the scheduler has updated
	static final long UPDATE_RATE = 300000L;

	private final NHLBot nhlBot;
	private final Guild guild;
	// Map<GamePk, GameDayChannel>
	private final Map<Integer, NHLGameDayChannelThread> gameDayChannels;

	Map<Integer, NHLGameDayChannelThread> getGameDayChannels() {
		return new ConcurrentHashMap<>(gameDayChannels);
	}

	boolean isGameDayChannelExist(int gamePk) {
		return gameDayChannels.get(gamePk) != null;
	}

	/**
	 * 
	 * @param guildId
	 * @param gamePk
	 * @return the removed GameDayChannel
	 */
	NHLGameDayChannelThread removeGameDayChannel(int gamePk) {
		NHLGameDayChannelThread gameDayChannel = gameDayChannels.remove(gamePk);
		if (gameDayChannel == null) {
			return null;
		}
		gameDayChannel.interrupt();

		return gameDayChannel;
	}

	private NHLGdcGuildManager(NHLBot nhlBot, Guild guild) {
		this.nhlBot = nhlBot;
		this.guild = guild;
		gameDayChannels = new ConcurrentHashMap<>();
	}

	public static NHLGdcGuildManager getAndStart(NHLBot nhlBot, Guild guild) {
		NHLGdcGuildManager manager = new NHLGdcGuildManager(nhlBot, guild);
		managers.put(guild.getId().asLong(), manager);
		manager.start();
		return manager;
	}

	public static NHLGdcGuildManager getManager(Long guildId) {
		return managers.get(guildId);
	}

	public static void removeManager(Long guildId) {
		NHLGdcGuildManager manager = managers.remove(guildId);
		if (manager != null) {
			manager.updateChannels();
			manager.interrupt();
		}
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
					sleepFor(INIT_UPDATE_RATE);
				} else if (lastUpdate == null || schedulerUpdate.compareTo(lastUpdate) > 0) {
					LOGGER.info("Updating Channels...");
					updateChannels();
					lastUpdate = schedulerUpdate;
				} else {
					LOGGER.debug("Waiting for GameScheduler to update...");
					sleepFor(UPDATE_RATE);
				}
			} catch (Exception e) {
				LOGGER.error("Error occured when updating channels.", e);
				sleepFor(UPDATE_RATE);
			}
		}
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
				game.getNiceName(), guild.getName());
		int gamePk = game.getGameId();
		long guildId = guild.getId().asLong();

		NHLGameDayChannelThread gameDayChannel = gameDayChannels.get(gamePk);
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
				gameTracker.getGame().getNiceName(), guild.getName());
		NHLGameDayChannelThread channel = NHLGameDayChannelThread.get(nhlBot, gameTracker, guild);
		gameDayChannels.put(gameTracker.getGame().getGameId(), channel);
		return channel;
	}

	public void updateChannels() {
		GuildPreferences preferences = nhlBot.getPersistentData().getPreferencesData()
				.getGuildPreferences(guild.getId().asLong());
		updateChannels(preferences);
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
	public void updateChannels(GuildPreferences preferences) {
		try {
			List<Team> teams = preferences.getTeams();

			LOGGER.info("Updating Channels for [{}]: activeGames={}",
					guild.getId().asLong(),
					nhlBot.getNHLGameScheduler().getActiveGames(teams).stream()
							.map(Game::getNiceName)
							.collect(Collectors.toList()));

			// Remove channels of outdated/unsubscribed games
			for (TextChannel channel : DiscordManager.getTextChannels(guild)) {
				if (isRemoveChannel(channel, preferences)) {
					deleteChannel(channel);
				}
			}

			for (Entry<Integer, NHLGameDayChannelThread> gdcEntry : gameDayChannels.entrySet()) {
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
	void deleteChannel(TextChannel channel) {
		LOGGER.info("Remove channel: " + channel.getName());
		Game game = nhlBot.getNHLGameScheduler().getGameByChannelName(channel.getName());
		if (game != null) {
			removeGameDayChannel(game.getGameId());
		}

		DiscordManager.deleteChannel(channel);
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

		// Checks if the channel corresponds to a real game
		if (nhlBot.getNHLGameScheduler().getGameByChannelName(channel.getName()) == null)
		{
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
		return channelName.matches(Config.NHL_CHANNEL_REGEX);
	}

	boolean isGameActive(List<Team> teams, String channelName) {
		return nhlBot.getNHLGameScheduler().isGameActive(teams, channelName);
	}
}
