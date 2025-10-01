package com.hazeluff.discord.bot.gdc.nhl.playoff;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMeta;
import com.hazeluff.discord.bot.database.channel.playoff.PlayoffWatchMeta;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.nhl.NHLGameTracker;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.game.Game;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.TextChannelCreateSpec;

public class PlayoffWatchChannel extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlayoffWatchChannel.class);

	public static final String CHANNEL_NAME = "playoffs-watch";

	// Poll for every 5 seconds, (On initialization)
	static final long INIT_UPDATE_RATE = 5000L;
	// Poll for every 5 minutes - if the scheduler has updated
	static final long UPDATE_RATE = 300000L;

	private final NHLBot nhlBot;
	private final Guild guild;
	private final TextChannel channel;

	// Map<GuildId, Map<GamePk, GameDayChannel>>
	private final Map<Integer, PlayoffWatchGameDayThread> gameDayThreads;
	private PlayoffWatchSummaryUpdater summaryUpdater;

	PlayoffWatchChannel(NHLBot nhlBot, Guild guild, TextChannel channel, PlayoffWatchMeta meta) {
		this.nhlBot = nhlBot;
		this.guild = guild;
		this.channel = channel;
		this.gameDayThreads = new ConcurrentHashMap<>();
		this.summaryUpdater = new PlayoffWatchSummaryUpdater(nhlBot, channel, meta);
	}

	public static PlayoffWatchChannel getOrCreateChannel(NHLBot nhlBot, Guild guild) {
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
				channelSpecBuilder.topic("Hockey is for everybody - except losers.");
				if (category != null) {
					channelSpecBuilder.parentId(category.getId());
				}
				channel = DiscordManager.createAndGetChannel(guild, channelSpecBuilder.build());
			}
		}
		
		PlayoffWatchMeta meta = null;
		if (channel != null) {
			meta = nhlBot.getPersistentData().getPlayoffWatchMetaData().loadMeta(channel.getId().asLong());
			if (meta == null) {
				meta = PlayoffWatchMeta.of(channel.getId().asLong());
			}
		}
		
		PlayoffWatchChannel fnChannel = new PlayoffWatchChannel(nhlBot, guild, channel, meta);

		if (channel != null) {
			fnChannel.start();
			fnChannel.summaryUpdater.start();
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
				LocalDate schedulerUpdate = nhlBot.getNHLGameScheduler().getLastUpdate();
				if (schedulerUpdate == null) {
					LOGGER.info("Waiting for GameScheduler to initialize...");
					Utils.sleep(INIT_UPDATE_RATE);
				} else if (lastUpdate == null || schedulerUpdate.compareTo(lastUpdate) > 0) {
					LOGGER.info("Updating Channels...");
					try {
						updateChannel();
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

	void updateChannel() {
		List<Game> activeGames = nhlBot.getNHLGameScheduler().getActivePlayoffGames();
		for (Game game : activeGames) {
			int gamePk = game.getGameId();
			NHLGameTracker gameTracker = nhlBot.getNHLGameScheduler().getGameTracker(game);
			if (gameTracker != null) {
				if (!gameDayThreads.containsKey(gamePk)) {
					PlayoffWatchGameDayThread gdt = PlayoffWatchGameDayThread.get(nhlBot, channel, gameTracker, guild);
					gameDayThreads.put(gamePk, gdt);
				}
			}
		}
		
		List<Game> inactivePlayoffGames = nhlBot.getNHLGameScheduler().getPlayoffGames().stream()
				.filter(game -> !activeGames.contains(game))
				.collect(Collectors.toList());
		for (Game inactiveGame : inactivePlayoffGames) {
			int gamePk = inactiveGame.getGameId();
			if (gameDayThreads.containsKey(gamePk)) {
				PlayoffWatchGameDayThread gdt = gameDayThreads.remove(gamePk);
				gdt.unpinSummaryMessage();
			} else {
				GDCMeta meta = nhlBot.getPersistentData().getGDCMetaData().loadMeta(
					channel.getId().asLong(),
					inactiveGame.getGameId()
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

	/**
	 * Used for stubbing the loop of {@link #run()} for tests.
	 * 
	 * @return
	 */
	boolean isStop() {
		return false;
	}
}
