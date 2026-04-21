package com.hazeluff.discord.bot.gdc.nhl.playoff;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.channel.playoff.PlayoffWatchMeta;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.nhl.NHLGameTracker;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.game.NHLGame;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.TextChannelCreateSpec;

public class NHLPlayoffWatchChannel extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(NHLPlayoffWatchChannel.class);

	public static final String CHANNEL_NAME = "playoffs";

	// Poll for every 5 seconds, (On initialization)
	static final long INIT_UPDATE_RATE = 5000L;
	// Poll for every 5 minutes - if the scheduler has updated
	static final long UPDATE_RATE = 300000L;

	private final NHLBot nhlBot;
	private final Guild guild;
	private final TextChannel channel;

	// Map<GuildId, Map<GamePk, GameDayChannel>>
	private final Map<Integer, NHLPlayoffWatchGameDayThread> gameDayThreads;
	private NHLPlayoffWatchSummaryUpdater summaryUpdater;

	private final static Map<Long, NHLPlayoffWatchChannel> channels = new ConcurrentHashMap<>();

	NHLPlayoffWatchChannel(NHLBot nhlBot, Guild guild, TextChannel channel, PlayoffWatchMeta meta) {
		this.nhlBot = nhlBot;
		this.guild = guild;
		this.channel = channel;
		this.gameDayThreads = new ConcurrentHashMap<>();
		this.summaryUpdater = new NHLPlayoffWatchSummaryUpdater(nhlBot, channel, meta);
	}

	public static NHLPlayoffWatchChannel getOrCreate(NHLBot nhlBot, Guild guild) {
		long guildId = guild.getId().asLong();
		if (channels.containsKey(guildId)) {
			return channels.get(guildId);
		}
		GuildPreferences pref = nhlBot.getPersistentData().getPreferencesData().getGuildPreferences(guildId);
		TextChannel channel = null;
		try {
			// Attempt to fetch channel by the saved preferences
			Long prefChannelId = pref.getPlayoffChannelId();
			if (prefChannelId != null) {
				nhlBot.getDiscordManager();
				channel = DiscordManager.getTextChannel(guild, prefChannelId);
			}
			if (channel == null) {
				channel = DiscordManager.getTextChannels(guild).stream()
					.filter(guildChannel -> guildChannel.getName().equals(CHANNEL_NAME))
					.findFirst()
					.orElse(null);
			}
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
				if (channel != null) {
					pref.setPlayoffChannelId(channel.getId().asLong());
					nhlBot.getPersistentData().getPreferencesData().savePreferences(guildId, pref);
				}
			}
		}
		
		PlayoffWatchMeta meta = null;
		if (channel != null) {
			meta = nhlBot.getPersistentData().getPlayoffWatchMetaData().loadMeta(channel.getId().asLong());
			if (meta == null) {
				meta = PlayoffWatchMeta.of(channel.getId().asLong());
			}
		}
		
		NHLPlayoffWatchChannel fnChannel = new NHLPlayoffWatchChannel(nhlBot, guild, channel, meta);
		fnChannel.summaryUpdater.saveMetadata();

		if (channel != null) {
			fnChannel.start();
			fnChannel.summaryUpdater.start();
		} else {
			LOGGER.warn("Channel could not be found in Discord.");
		}

		channels.put(guildId, fnChannel);
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
		List<NHLGame> activeGames = nhlBot.getNHLGameScheduler().getActivePlayoffGames();
		for (NHLGame game : activeGames) {
			int gamePk = game.getGameId();
			NHLGameTracker gameTracker = nhlBot.getNHLGameScheduler().getGameTracker(game);
			if (gameTracker != null) {
				if (!gameDayThreads.containsKey(gamePk)) {
					NHLPlayoffWatchGameDayThread gdt = NHLPlayoffWatchGameDayThread.getOrCreate(
						nhlBot, channel, gameTracker, guild, false);
					gameDayThreads.put(gamePk, gdt);
				}
			}
		}
		
		/*
		List<Game> inactivePlayoffGames = nhlBot.getNHLGameScheduler().getPlayoffGames().stream()
				.filter(game -> !activeGames.contains(game))
				.collect(Collectors.toList());
		for (Game inactiveGame : inactivePlayoffGames) {
			int gamePk = inactiveGame.getGameId();
			if (gameDayThreads.containsKey(gamePk)) {
				PlayoffWatchGameDayThread gdt = gameDayThreads.remove(gamePk);
				gdt.unpinSummaryMessage();
			} else {
				GDCMeta meta = nhlBot.getPersistentData().getGDCMetaData().loadMetaByChannelId(
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
		*/
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
