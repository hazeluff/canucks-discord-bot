package com.hazeluff.discord.bot.gdc.ahl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.ahl.game.Game;
import com.hazeluff.discord.ahl.AHLGameTracker;
import com.hazeluff.discord.ahl.AHLTeams.Team;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMeta;
import com.hazeluff.discord.bot.database.channel.playoff.PlayoffWatchMeta;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.utils.Utils;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;

public class AHLWatchChannel extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(AHLWatchChannel.class);

	private static final Team TEAM = Team.ABBY_NUCKS;
	public static final String CHANNEL_NAME = "abby-playoffs";

	// Poll for every 5 seconds, (On initialization)
	static final long INIT_UPDATE_RATE = 5000L;
	// Poll for every 5 minutes - if the scheduler has updated
	static final long UPDATE_RATE = 300000L;

	private final NHLBot nhlBot;
	private final Guild guild;
	private final TextChannel channel;

	// Map<GuildId, Map<GamePk, GameDayChannel>>
	private final Map<Integer, AHLGameDayThread> regularGameDayThreads;
	// Map<GuildId, Map<GamePk, GameDayChannel>>
	private final Map<Integer, AHLGameDayThread> playoffGameDayThreads;

	AHLWatchChannel(NHLBot nhlBot, Guild guild, TextChannel channel, PlayoffWatchMeta meta) {
		this.nhlBot = nhlBot;
		this.guild = guild;
		this.channel = channel;
		this.regularGameDayThreads = new ConcurrentHashMap<>();
		this.playoffGameDayThreads = new ConcurrentHashMap<>();
	}

	public static AHLWatchChannel createChannel(NHLBot nhlBot, Guild guild) {
		TextChannel channel = null;
		try {
			channel = guild.getChannels().filter(TextChannel.class::isInstance).cast(TextChannel.class)
					.filter(guildChannel -> guildChannel.getName().equals(CHANNEL_NAME))
					.take(1)
					.onErrorReturn(null)
					.blockFirst();
		} catch (Exception e) {
			LOGGER.warn("Problem fetching existing channel.");
		} finally {
			if (channel == null) {
				LOGGER.warn("Channel not found/error.");
				channel = DiscordManager.createAndGetChannel(guild, CHANNEL_NAME);
			}
		}
		
		PlayoffWatchMeta meta = null;
		if (channel != null) {
			meta = nhlBot.getPersistentData().getPlayoffWatchMetaData().loadMeta(channel.getId().asLong());
			if (meta == null) {
				meta = PlayoffWatchMeta.of(channel.getId().asLong());
			}
		}
		
		AHLWatchChannel fnChannel = new AHLWatchChannel(nhlBot, guild, channel, meta);

		if (channel != null) {
			fnChannel.start();
		} else {
			LOGGER.warn("Channel could not be found in Discord.");
		}

		return fnChannel;
	}

	@Override
	public void run() {

		LocalDate lastUpdate = null;
		while (!isStop()) {
			try {
				LocalDate schedulerUpdate = nhlBot.getAHLGameScheduler().getLastUpdate();
				if (schedulerUpdate == null) {
					LOGGER.info("Waiting for GameScheduler to initialize...");
					Utils.sleep(INIT_UPDATE_RATE);
				} else if (lastUpdate == null || schedulerUpdate.compareTo(lastUpdate) > 0) {
					LOGGER.info("Updating Channels...");
					try {
						updateGameThreads();
					} catch (Exception e) {
						LOGGER.warn("Failed to update channel.", e);
					}
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

	void updateGameThreads() {
		updateRegularGameThreads();
		updatePlayoffGameThreads();
	}

	private void updateRegularGameThreads() {
		List<Game> activeGames = nhlBot.getAHLGameScheduler().getActiveGames(TEAM);
		if (!nhlBot.getAHLGameScheduler().getActivePlayoffGames(TEAM).isEmpty() && activeGames.size() == 1) {
			// No current/future game. (only past game)
			activeGames.clear(); // Treat the past game as non-active, so it is removed as inactive
		}
		for (Game game : activeGames) {
			int gamePk = game.getId();
			AHLGameTracker gameTracker = nhlBot.getAHLGameScheduler().getGameTracker(game);
			if (gameTracker != null) {
				if (!regularGameDayThreads.containsKey(gamePk)) {
					AHLGameDayThread gdt = AHLGameDayThread.get(nhlBot, channel, gameTracker, guild);
					regularGameDayThreads.put(gamePk, gdt);
				}
			}
		}
		List<Game> inactiveGames = nhlBot.getAHLGameScheduler().getRegularGames(TEAM).stream()
				.filter(game -> !activeGames.contains(game))
				.collect(Collectors.toList());
		for (Game inactiveGame : inactiveGames) {
			int gameId = inactiveGame.getId();
			if (regularGameDayThreads.containsKey(gameId)) {
				AHLGameDayThread gdt = regularGameDayThreads.remove(gameId);
				gdt.unpinSummaryMessage();
			} else {
				GDCMeta meta = nhlBot.getPersistentData().getGDCMetaData().loadMeta(
					channel.getId().asLong(),
						inactiveGame.getId()
				);
				if (meta != null) {
					Long messageId = meta.getSummaryMessageId();
					if (messageId != null) {
						Message message = nhlBot.getDiscordManager().getMessage(channel.getId().asLong(), messageId);
						if (message != null) {
							DiscordManager.unpinMessage(message);
						}
					}
				}
			}
		}
	}

	private void updatePlayoffGameThreads() {
		List<Game> activeGames = nhlBot.getAHLGameScheduler().getActivePlayoffGames(TEAM);
		for (Game game : activeGames) {
			int gamePk = game.getId();
			AHLGameTracker gameTracker = nhlBot.getAHLGameScheduler().getGameTracker(game);
			if (gameTracker != null) {
				if (!playoffGameDayThreads.containsKey(gamePk)) {
					AHLGameDayThread gdt = AHLGameDayThread.get(nhlBot, channel, gameTracker, guild);
					playoffGameDayThreads.put(gamePk, gdt);
				}
			}
		}
		List<Game> inactiveGames = nhlBot.getAHLGameScheduler().getPlayoffGames(TEAM).stream()
				.filter(game -> !activeGames.contains(game)).collect(Collectors.toList());
		for (Game inactiveGame : inactiveGames) {
			int gameId = inactiveGame.getId();
			if (playoffGameDayThreads.containsKey(gameId)) {
				AHLGameDayThread gdt = playoffGameDayThreads.remove(gameId);
				gdt.unpinSummaryMessage();
			} else {
				GDCMeta meta = nhlBot.getPersistentData().getGDCMetaData().loadMeta(channel.getId().asLong(),
						inactiveGame.getId());
				if (meta != null) {
					Long messageId = meta.getSummaryMessageId();
					if (messageId != null) {
						Message message = nhlBot.getDiscordManager().getMessage(channel.getId().asLong(), messageId);
						if (message != null) {
							DiscordManager.unpinMessage(message);
						}
					}
				}
			}
		}
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
