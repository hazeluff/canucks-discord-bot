package com.hazeluff.discord.bot.gdc.fournations;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.nhl.GameTracker;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.game.Game;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;

public class FourNationsChannel extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(FourNationsChannel.class);

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

	FourNationsChannel(NHLBot nhlBot, Guild guild, TextChannel channel) {
		this.nhlBot = nhlBot;
		this.guild = guild;
		this.channel = channel;
		this.gameDayThreads = new ConcurrentHashMap<>();
	}

	public static FourNationsChannel createChannel(NHLBot nhlBot, Guild guild) {
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
		FourNationsChannel fnChannel = new FourNationsChannel(nhlBot, guild, channel);
		fnChannel.start();
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
				LocalDate schedulerUpdate = nhlBot.getGameScheduler().getLastUpdate();
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
		List<Game> games = nhlBot.getGameScheduler().getFourNationsGames();
		for (Game game : games) {
			int gamePk = game.getGameId();
			GameTracker gameTracker = nhlBot.getGameScheduler().getFourNationsGameTracker(game);
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
