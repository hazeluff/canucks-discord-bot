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
import com.hazeluff.discord.bot.command.WordcloudCommand;
import com.hazeluff.discord.bot.command.gdc.GDCStatsCommand;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMeta;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.bot.gdc.nhl.custom.game.CustomGameMessages;
import com.hazeluff.discord.nhl.NHLGameTracker;
import com.hazeluff.nhl.game.Game;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Category;
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


	public NHLGameDayChannelThread(NHLBot nhlBot, NHLGameTracker gameTracker, Guild guild, TextChannel textChannel,
			GuildPreferences preferences, GDCMeta meta) {
		super(nhlBot, gameTracker, guild, textChannel, preferences, meta);
	}

	public static NHLGameDayChannelThread get(NHLBot nhlBot, NHLGameTracker gameTracker, Guild guild) {
		GuildPreferences preferences = nhlBot.getPersistentData().getPreferencesData()
				.getGuildPreferences(guild.getId().asLong());
		TextChannel textChannel = getTextChannel(guild, gameTracker.getGame(), nhlBot, preferences);
		GDCMeta meta = null;
		if (textChannel != null) {
			meta = nhlBot.getPersistentData().getGDCMetaData().loadMeta(
				textChannel.getId().asLong(),
				gameTracker.getGame().getGameId()
			);
			if (meta == null) {
				meta = GDCMeta.of(textChannel.getId().asLong(), gameTracker.getGame().getGameId());
			}
		}
		NHLGameDayChannelThread gameDayChannel = new NHLGameDayChannelThread(nhlBot, gameTracker, guild, textChannel, preferences, meta);

		if (gameDayChannel.channel != null) {
			gameDayChannel.start();
		} else {
			LOGGER.warn("GameDayChannel not started. TextChannel could not be found. guild={}",
					guild.getId().asLong());
		}
		return gameDayChannel;
	}

	static TextChannel getTextChannel(Guild guild, Game game, NHLBot nhlBot, GuildPreferences preferences) {
		TextChannel channel = null;
		try {
			String channelName = game.getNiceName();
			Predicate<TextChannel> channelMatcher = c -> c.getName().equalsIgnoreCase(channelName);
			Category category = nhlBot.getGdcCategoryManager().get(guild);
			if (!DiscordManager.getTextChannels(guild).stream().anyMatch(channelMatcher)) {
				if (game.getGameState().isFinished()) {
					return null;
				}
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

	/*
	 * Run method overrides
	 */
	@Override
	protected void initChannel() {
		loadMetadata();
		initIntroMessage();
		initSummaryMessage();
		updateSummaryMessage();
	}

	@Override
	protected void updateActive() {
		updateMessages();
		updateSummaryMessage();
	}

	@Override
	protected void updateEnd() {
		sendEndOfGameMessage();
		sendStatsMessage();
		sendCustomEndMessage();
		sendWordcloud();
	}

	@SuppressWarnings("serial")
	@Override
	protected Map<Long, String> getReminders() {
		return new HashMap<Long, String>() {
			{
				put(3600000l, "60 minutes till puck drop.");
				put(1800000l, "30 minutes till puck drop.");
				put(600000l, "10 minutes till puck drop.");
			}
		};
	}

	protected void sendStatsMessage() {
		try {
			Message statsGameMessage = null;
			if (channel != null) {
				EmbedCreateSpec embedSpec = GDCStatsCommand.buildEmbed(game);
				MessageCreateSpec msgSpec = MessageCreateSpec.builder().addEmbed(embedSpec).build();
				statsGameMessage = DiscordManager.sendAndGetMessage(channel, msgSpec);
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
		Message endOfGameMessage = null;
		try {
			if (channel != null) {
				endOfGameMessage = DiscordManager.sendAndGetMessage(channel, buildEndOfGameMessage());
			}
			if (endOfGameMessage != null) {
				LOGGER().debug("Sent end of game message for game. Pinning it...");
				DiscordManager.pinMessage(endOfGameMessage);
			}
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

		List<Game> nextGames = preferences.getTeams().stream()
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
			if (channel != null && message != null) {
				sendMessage(message);
			}
		} catch (Exception e) {
			LOGGER().error("Could not send EoG Custom Message.");
		}
	}

	protected void sendWordcloud() {
		try {
			new WordcloudCommand(nhlBot).sendWordcloud(channel, game);
		} catch (Exception e) {
			LOGGER().error("Could not send Wordcloud.");
		}
	}
}
