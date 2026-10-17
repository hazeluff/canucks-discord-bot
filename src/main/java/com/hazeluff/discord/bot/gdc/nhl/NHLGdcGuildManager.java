package com.hazeluff.discord.bot.gdc.nhl;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.nhl.NHLGameTracker;
import com.hazeluff.discord.nhl.NHLTeams.Team;
import com.hazeluff.discord.utils.InterruptableThread;
import com.hazeluff.nhl.game.NHLGame;

import discord4j.core.object.entity.Guild;
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
	// Poll for every 30 minutes - if the scheduler has updated
	static final long UPDATE_RATE = 1800000L;
	public static final String NHL_CHANNEL_REGEX;
	static {
		String teamRegex = String.join("|", Arrays.asList(Team.values()).stream()
			.map(team -> team.getCode().toLowerCase()).collect(Collectors.toList()));
		teamRegex = String.format("(%s)", teamRegex);
		String startYear = String.valueOf(Config.NHL_CURRENT_SEASON.getStartYear()).substring(2, 4);
		String endYear = String.valueOf(Config.NHL_CURRENT_SEASON.getEndYear()).substring(2, 4);
		NHL_CHANNEL_REGEX = String.format("%1$s-vs-%1$s-(%2$s|%3$s)-[0-9]{2}-[0-9]{2}", teamRegex, startYear, endYear);
	}


	private final NHLBot nhlBot;
	private final Guild guild;
	private final long guildId;
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
		this.guildId = guild.getId().asLong();
		gameDayChannels = new ConcurrentHashMap<>();
	}

	public static NHLGdcGuildManager getAndStart(NHLBot nhlBot, Guild guild) {
		long guildId = guild.getId().asLong();
		if (managers.containsKey(guildId))
			return managers.get(guildId);

		NHLGdcGuildManager manager = new NHLGdcGuildManager(nhlBot, guild);
		managers.put(guildId, manager);
		manager.start();
		return manager;
	}

	public static NHLGdcGuildManager getManager(Long guildId) {
		return managers.get(guildId);
	}

	public static void removeManager(Long guildId) {
		NHLGdcGuildManager manager = managers.remove(guildId);
		if (manager != null) {
			manager.interrupt();
		}
	}

	@Override
	public void run() {
		setName(String.valueOf(guildId));
		LOGGER.info("GameDayChannelsManager Thread started.");
		LocalDate lastUpdate = null;
		while (!isStop()) {
			try {
				LocalDate schedulerUpdate = nhlBot.getNHLGameScheduler().getLastUpdate();
				if (schedulerUpdate == null) {
					LOGGER.info("Waiting for GameScheduler to initialize...");
					sleepFor(INIT_UPDATE_RATE);
				} else if (lastUpdate == null || schedulerUpdate.compareTo(lastUpdate) > 0) {
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
	public NHLGameDayChannelThread createChannel(NHLGame game) {
		int gamePk = game.getGameId();
		long guildId = guild.getId().asLong();

		NHLGameDayChannelThread gameDayChannel = gameDayChannels.get(gamePk);
		if (gameDayChannel == null) {
			NHLGameTracker gameTracker = nhlBot.getNHLGameScheduler().getGameTracker(game);
			if (gameTracker != null) {
				gameDayChannel = createGameDayChannel(nhlBot, gameTracker);
			} else {
				LOGGER.error("Could not find GameTracker for game [{}]", game);
			}
		} else {
			LOGGER.debug("Game Day Channel already exists for game [{}] in guild [{}]", gamePk, guildId);
		}

		return gameDayChannel;
	}

	NHLGameDayChannelThread createGameDayChannel(NHLBot nhlBot, NHLGameTracker gameTracker) {
		LOGGER.info("Creating ChannelThread. channelName={}", gameTracker.getGame().getNiceName());
		NHLGameDayChannelThread channel = NHLGameDayChannelThread.getOrCreate(nhlBot, gameTracker, guild);
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
			List<NHLGame> activeGames = nhlBot.getNHLGameScheduler().getActiveGames(teams);
			LOGGER.info("Updating Channels: activeGames={}",
				activeGames.stream().map(NHLGame::getNiceName).collect(Collectors.toList()));

			// Remove channels of outdated/unsubscribed games
			List<TextChannel> channels = DiscordManager.getTextChannels(guild);

			List<TextChannel> channelsToDelete = channels.stream()
				.filter(channel -> isRemoveChannel(channel, preferences))
				.collect(Collectors.toList());
			LOGGER.info("Cleanup Channels: channels={},channelsToDelete={}", channels.size(), channelsToDelete.size());
			for (TextChannel channel : channelsToDelete) {
				deleteChannel(channel);
			}

			for (Entry<Integer, NHLGameDayChannelThread> gdcEntry : gameDayChannels.entrySet()) {
				NHLGameDayChannelThread gdc = gdcEntry.getValue();
				gdc.updateIntroMessage();
			}

			// Create game channels of latest game for current subscribed team
			for (NHLGame game : activeGames) {
				createChannel(game);
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
		NHLGame game = nhlBot.getNHLGameScheduler().getGameByChannelName(channel.getName());
		if (game != null) {
			removeGameDayChannel(game.getGameId());
		}

		DiscordManager.deleteChannel(channel);
	}

	boolean isRemoveChannel(TextChannel channel, GuildPreferences preferences) {
		// Does not remove channels that does not have the correct name format
		if (!isChannelNameFormat(channel.getName())) {
			return false;
		}

		// Does not remove active games
		if (preferences != null && isGameActive(preferences.getTeams(), channel.getName())) {
			return false;
		}

		return true;
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
		return channelName.matches(NHL_CHANNEL_REGEX);
	}

	boolean isGameActive(List<Team> teams, String channelName) {
		return nhlBot.getNHLGameScheduler().isGameActive(teams, channelName);
	}
}
