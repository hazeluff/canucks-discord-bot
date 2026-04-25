package com.hazeluff.discord.bot.gdc.nhl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.gdc.GDCStatsCommand;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMeta;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.bot.gdc.nhl.custom.game.CustomGameMessages;
import com.hazeluff.discord.nhl.NHLGameTracker;
import com.hazeluff.nhl.game.NHLGame;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.TextChannelCreateSpec;

public class NHLGameDayChannelThread extends NHLGameDayThread {
	private static final Logger LOGGER = LoggerFactory.getLogger(NHLGameDayChannelThread.class);

	@Override
	protected Logger LOGGER() {
		return LOGGER;
	}

	public NHLGameDayChannelThread(NHLBot nhlBot, NHLGameTracker gameTracker, Guild guild, MessageChannel channel,
		MessageChannel parentChannel, GDCMeta meta) {
		super(nhlBot, gameTracker, guild, channel, parentChannel, meta, false);
	}

	public static NHLGameDayChannelThread getOrCreate(NHLBot nhlBot, NHLGameTracker gameTracker, Guild guild) {
		long guildId = guild.getId().asLong();
		GuildPreferences preferences = nhlBot.getPersistentData().getPreferencesData()
				.getGuildPreferences(guildId);
		TextChannel textChannel = getTextChannel(guild, gameTracker.getGame(), nhlBot, preferences);
		GDCMeta meta = null;
		if (textChannel != null) {
			long channelId = textChannel.getId().asLong();
			int gameId = gameTracker.getGame().getGameId();
			meta = nhlBot.getPersistentData().getGDCMetaData().loadMetaByChannelId(channelId, gameId);
			if (meta == null) {
				meta = GDCMeta.forChannel(channelId, gameTracker.getGame().getGameId());
			}
		}
		NHLGameDayChannelThread gameDayChannel = new NHLGameDayChannelThread(nhlBot, gameTracker, guild, textChannel,
			textChannel, meta);

		if (gameDayChannel.threadChannel != null) {
			gameDayChannel.start();
		} else {
			LOGGER.warn("GameDayChannel not started. TextChannel could not be found. guild={}",
					guild.getId().asLong());
		}
		return gameDayChannel;
	}

	static TextChannel getTextChannel(Guild guild, NHLGame game, NHLBot nhlBot, GuildPreferences preferences) {
		TextChannel channel = null;
		try {
			String channelName = game.getNiceName();
			Predicate<TextChannel> channelMatcher = c -> c.getName().equalsIgnoreCase(channelName);
			Category category = nhlBot.getGdcCategoryManager().get(guild);
			if (!DiscordManager.getTextChannels(guild).stream().anyMatch(channelMatcher)) {
				if (game.getGameState().isFinished()) {
					LOGGER.debug("Game for [" + channelName + "] already finished. Skipping channel creation.");
					return null;
				}

				LOGGER.debug("Channel [" + channelName + "] does not exist in [" + guild.getName() + "]");
				TextChannelCreateSpec.Builder channelSpecBuilder = TextChannelCreateSpec.builder();
				channelSpecBuilder.name(channelName);
				channelSpecBuilder.topic(preferences.getCheer());
				if (category != null) {
					channelSpecBuilder.parentId(category.getId());
				}
				channel = DiscordManager.createAndGetChannel(guild, channelSpecBuilder.build());
			} else {
				LOGGER.debug("Channel [" + channelName + "] already exists in [" + guild.getName() + "]");
				channel = DiscordManager.getTextChannels(guild).stream().filter(channelMatcher).findAny().orElse(null);

				if (category != null && !channel.getCategoryId().isPresent()) {
					DiscordManager.moveChannel(category, channel);
				}
			}

		} catch (Exception e) {
			LOGGER.error("Failed to create channel.", e);
		}
		return channel;
	}

	@Override
	protected boolean isGameInit() {
		return game.isInit();
	}

	/*
	 * Run method overrides
	 */
	@Override
	protected void initChannel() {
		loadMetadata();
		initIntroMessage();
		saveMetadata();
	}

	@Override
	protected void updateActive() {
		updateMessages();
	}

	@Override
	protected void updateEnd() {
		sendEndOfGameMessage();
		Message summaryMessage = sendSummaryMessage(); // Avoid save/load of metadata
		DiscordManager.pinMessage(summaryMessage);
	}

	@Override
	protected Map<Long, String> getReminders() {
		return new HashMap<Long, String>() {
			{
				put(3600000l, "60 minutes till puck drop.");
			}
		};
	}

	protected void sendStatsMessage() {
		try {
			Message statsGameMessage = null;
			if (threadChannel != null) {
				EmbedCreateSpec embedSpec = GDCStatsCommand.buildEmbed(game);
				MessageCreateSpec msgSpec = MessageCreateSpec.builder().addEmbed(embedSpec).build();
				statsGameMessage = DiscordManager.sendAndGetMessage(threadChannel, msgSpec);
			}
			if (statsGameMessage != null) {
				LOGGER().debug("Sent stats for the game. Pinning it...");
				DiscordManager.pinMessage(statsGameMessage);
			}
		} catch (Exception e) {
			LOGGER().error("Could not send Stats Message.");
		}
	}

	/*
	 * End of game message
	 */
	/**
	 * Sends the end of game message.
	 */
	protected void sendEndOfGameMessage() {
		try {
			DiscordManager.sendMessage(threadChannel, buildEndOfGameMessage());
		} catch (Exception e) {
			LOGGER().error("Could not send end of game Message.");
		}
	}

	/**
	 * Builds the message that is sent at the end of the game.
	 * 
	 * @param game
	 *            the game to build the message for
	 * @param team
	 *            team to specialize the message for
	 * @return end of game message
	 */
	@Override
	protected String buildEndOfGameMessage() {
		String message = "Game has ended. Thanks for joining!\n" + "Final Score: " + buildGameScore(game);

		GuildPreferences preferences = nhlBot.getPersistentData().getPreferencesData()
			.getGuildPreferences(guild.getId().asLong());
		List<NHLGame> nextGames = preferences.getTeams().stream()
				.map(team -> nhlBot.getNHLGameScheduler().getNextGame(team)).filter(Objects::nonNull)
				.collect(Collectors.toList());

		if (!nextGames.isEmpty()) {
			if (nextGames.size() > 1) {

			} else {
				message += "\nThe next game is: " + buildDetailsMessage(nextGames.get(0));
			}
		}
		return message;
	}

	protected void sendCustomEndMessage() {
		try {
			String message = CustomGameMessages.getMessage(game);
			if (threadChannel != null && message != null) {
				sendMessage(message);
			}
		} catch (Exception e) {
			LOGGER().error("Could not send EoG Custom Message.");
		}
	}

	public void stopAndRemoveGuildChannel() {
		DiscordManager.deleteChannel(threadChannel);
		interrupt();
	}
}
