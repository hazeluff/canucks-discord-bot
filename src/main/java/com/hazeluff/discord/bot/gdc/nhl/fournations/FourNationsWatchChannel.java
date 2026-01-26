package com.hazeluff.discord.bot.gdc.nhl.fournations;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.nhl.NHLGameTracker;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.game.Game;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.TextChannelCreateSpec;

public class FourNationsWatchChannel extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(FourNationsWatchChannel.class);

	public static final String CHANNEL_NAME = "four-nations";

	// Poll for every 5 seconds, (On initialization)
	static final long INIT_UPDATE_RATE = 5000L;
	// Poll for every 5 minutes - if the scheduler has updated
	static final long UPDATE_RATE = 300000L;

	private final NHLBot nhlBot;
	private final Guild guild;
	private final TextChannel channel;

	// Map<GuildId, Map<GamePk, GameDayChannel>>
	private final Map<Integer, FourNationsGameDayThread> gameDayThreads;

	private final static Map<Long, FourNationsWatchChannel> channels = new ConcurrentHashMap<>();

	FourNationsWatchChannel(NHLBot nhlBot, Guild guild, TextChannel channel) {
		this.nhlBot = nhlBot;
		this.guild = guild;
		this.channel = channel;
		this.gameDayThreads = new ConcurrentHashMap<>();
	}

	public static FourNationsWatchChannel getOrCreateChannel(NHLBot nhlBot, Guild guild) {
		long guildId = guild.getId().asLong();
		if (channels.containsKey(guildId)) {
			return channels.get(guildId);
		}
		TextChannel channel = null;
		try {
			channel = DiscordManager.getTextChannels(guild).stream()
					.filter(guildChannel -> guildChannel.getName().equals(CHANNEL_NAME))
					.findFirst()
					.orElse(null);
		} catch (Exception e) {
			LOGGER.warn("Problem fetching existing channel.");
		} finally {
			if (channel == null) {
				LOGGER.warn("Channel not found/error.");
				Category category = nhlBot.getNHLBotCategoryManager().get(guild);
				TextChannelCreateSpec.Builder channelSpecBuilder = TextChannelCreateSpec.builder();
				channelSpecBuilder.name(CHANNEL_NAME);
				channelSpecBuilder.topic("Four Nations - Facing Off!");
				if (category != null) {
					channelSpecBuilder.parentId(category.getId());
				}
				channel = DiscordManager.createAndGetChannel(guild, channelSpecBuilder.build());
			}
		}
		FourNationsWatchChannel fnChannel = new FourNationsWatchChannel(nhlBot, guild, channel);
		fnChannel.start();
		channels.put(guildId, fnChannel);
		return fnChannel;
	}

	@Override
	public void run() {
		if (channel == null) {
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
					LOGGER.info("Updating Channels...");
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

	void updateChannel() {
		List<Game> games = nhlBot.getNHLGameScheduler().getFourNationsGames();
		for (Game game : games) {
			int gamePk = game.getGameId();
			NHLGameTracker gameTracker = nhlBot.getNHLGameScheduler().getFourNationsGameTracker(game);
			if (!game.getGameState().isFinished()) {
				// Start/Maintain gdc if they have not finished.
				FourNationsGameDayThread gdt = null;
				if (!gameDayThreads.containsKey(gamePk)) {
					gdt = FourNationsGameDayThread.get(nhlBot, channel, gameTracker, guild);
					gameDayThreads.put(gamePk, gdt);
				}
			} else {
				// Terminate finished gdc threads.
				if (gameDayThreads.containsKey(gamePk)) {
					FourNationsGameDayThread gdt = gameDayThreads.remove(gamePk);
					gdt.interrupt();
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
