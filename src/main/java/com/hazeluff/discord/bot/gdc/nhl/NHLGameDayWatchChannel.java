package com.hazeluff.discord.bot.gdc.nhl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.nhl.NHLGameTracker;
import com.hazeluff.discord.nhl.NHLTeams.Team;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.game.Game;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.TextChannelCreateSpec;

public class NHLGameDayWatchChannel extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(NHLGameDayWatchChannel.class);

	public static final String CHANNEL_NAME = "game-day-watch";

	// Poll for every 5 seconds, (On initialization)
	static final long INIT_UPDATE_RATE = 5000L;
	// Poll for every 5 minutes - if the scheduler has updated
	static final long UPDATE_RATE = 300000L;

	private final NHLBot nhlBot;
	private final Guild guild;
	private final TextChannel textChannel;

	// Map<GuildId, Map<GamePk, GameDayChannel>>
	private final Map<Integer, NHLGameDayWatchThread> gameDayThreads;

	private final static Map<Long, NHLGameDayWatchChannel> channels = new ConcurrentHashMap<>();

	NHLGameDayWatchChannel(NHLBot nhlBot, Guild guild, TextChannel channel) {
		this.nhlBot = nhlBot;
		this.guild = guild;
		this.textChannel = channel;
		this.gameDayThreads = new ConcurrentHashMap<>();
	}

	public static NHLGameDayWatchChannel getOrCreateChannel(NHLBot nhlBot, Guild guild) {
		long guildId = guild.getId().asLong();
		if (channels.containsKey(guildId)) {
			return channels.get(guildId);
		}
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
				Category category = nhlBot.getNHLBotCategoryManager().get(guild);
				TextChannelCreateSpec.Builder channelSpecBuilder = TextChannelCreateSpec.builder();
				channelSpecBuilder.name(CHANNEL_NAME);
				channelSpecBuilder.topic("Thank you for using! - Hazeluff");
				if (category != null) {
					channelSpecBuilder.parentId(category.getId());
				}
				channel = DiscordManager.createAndGetChannel(guild, channelSpecBuilder.build());
			}
		}
		NHLGameDayWatchChannel fnChannel = new NHLGameDayWatchChannel(nhlBot, guild, channel);
		fnChannel.start();
		channels.put(guildId, fnChannel);
		return fnChannel;
	}

	public static void removeChannel(Guild guild) {
		NHLGameDayWatchChannel channel = getChannel(guild);
		if(channel != null) {
			channels.remove(guild.getId().asLong());
			DiscordManager.deleteChannel(channel.textChannel);
		}
	}

	public static NHLGameDayWatchChannel getChannel(Guild guild) {
		return channels.get(guild.getId().asLong());
	}

	@Override
	public void run() {
		if (textChannel == null) {
			LOGGER.warn("Channel could not be found in Discord.");
			return;
		}

		LocalDate lastUpdate = null;
		while (!isStop()) {
			try {
				LocalDate schedulerUpdate = nhlBot.getNHLGameScheduler().getLastUpdate();
				if (schedulerUpdate == null) {
					LOGGER.info("Waiting for GameScheduler to initialize...");
					Utils.sleep(INIT_UPDATE_RATE);
				} else if (lastUpdate == null || schedulerUpdate.compareTo(lastUpdate) > 0) {
					LOGGER.info("Updating Channel...");
					updateChannel();
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

	public void updateChannel() {
		List<Team> teams = nhlBot.getPersistentData().getPreferencesData()
				.getGuildPreferences(guild.getId().asLong())
				.getTeams();
		List<Game> games = nhlBot.getNHLGameScheduler().getActiveGames(teams);
		for (Game game : games) {
			int gamePk = game.getGameId();
			NHLGameTracker gameTracker = nhlBot.getNHLGameScheduler().getGameTracker(game);
			if (!game.getGameState().isFinished()) {
				// Start/Maintain gdc if they have not finished.
				if (!gameDayThreads.containsKey(gamePk)) {
					NHLGameDayWatchThread gdt = NHLGameDayWatchThread.get(nhlBot, textChannel, gameTracker, guild);
					gameDayThreads.put(gamePk, gdt);
				}
			} else {
				// Terminate finished gdc threads.
				if (gameDayThreads.containsKey(gamePk)) {
					NHLGameDayWatchThread gdt = gameDayThreads.remove(gamePk);
					gdt.interrupt();
				}
			}
		}
		
		Predicate<? super Entry<Integer, NHLGameDayWatchThread>> isNoGameIdMatch = 
				entry -> games.stream().noneMatch(game -> game.getGameId() == entry.getKey());
		gameDayThreads.entrySet().stream()
			.filter(isNoGameIdMatch)
			.forEach(entry -> entry.getValue().interrupt());
		gameDayThreads.entrySet()
			.removeIf(isNoGameIdMatch);
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
