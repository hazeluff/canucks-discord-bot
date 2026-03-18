package com.hazeluff.discord.bot.gdc.nhl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

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
import com.hazeluff.discord.nhl.NHLTeams.Team;
import com.hazeluff.nhl.game.Game;

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


	public NHLGameDayChannelThread(NHLBot nhlBot, NHLGameTracker gameTracker, Guild guild, MessageChannel textChannel,
		GDCMeta meta) {
		super(nhlBot, gameTracker, guild, textChannel, meta);
	}

	public static NHLGameDayChannelThread get(NHLBot nhlBot, NHLGameTracker gameTracker, Guild guild) {
		long guildId = guild.getId().asLong();
		GuildPreferences preferences = nhlBot.getPersistentData().getPreferencesData().getGuildPreferences(guildId);
		TextChannel textChannel = getTextChannel(guild, gameTracker.getGame(), nhlBot, preferences);
		GDCMeta meta = null;
		if (textChannel != null) {
			long channelId = textChannel.getId().asLong();
			int gameId = gameTracker.getGame().getGameId();
			meta = nhlBot.getPersistentData().getGDCMetaData().loadMetaByChannelId(channelId, gameId);
			if (meta == null) {
				meta = GDCMeta.forChannel(channelId, gameId);
			}
		}
		NHLGameDayChannelThread gameDayChannel = new NHLGameDayChannelThread(nhlBot, gameTracker, guild, textChannel,
			meta);

		if (gameDayChannel.channel != null) {
			gameDayChannel.start();
		} else {
			LOGGER.warn("GameDayChannel not started. TextChannel could not be found. guild={}",
					guild.getId().asLong());
		}
		return gameDayChannel;
	}

	protected static TextChannel getTextChannel(Guild guild, Game game, NHLBot nhlBot, GuildPreferences preferences) {
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
		saveMetadata();
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
	 * Start of game message
	 */
	protected void updateStart() {
		sendStartOfGameMessage();
		sendCustomStartMessage();
	}

	protected void sendCustomStartMessage() {
		try {
			String message = CustomGameMessages.getStartGameMessage(game, Team.VANCOUVER_CANUCKS);
			if (channel != null && message != null) {
				sendMessage(message);
			}
		} catch (Exception e) {
			LOGGER().error("Could not send SoG Custom Message.");
		}
	}

	/*
	 * End of game message
	 */
	/**
	 * Sends the end of game message.
	 */
	@Override
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

		Game nextGame = nhlBot.getNHLGameScheduler().getNextGame(Team.VANCOUVER_CANUCKS);
		if (nextGame != null) {
			message += "\nThe next game is: " + buildDetailsMessage(nextGame);
		}
		return message;
	}

	protected void sendCustomEndMessage() {
		try {
			String message = CustomGameMessages.getEndGameMessage(game, Team.VANCOUVER_CANUCKS);
			if (channel != null && message != null) {
				sendMessage(message);
			}
		} catch (Exception e) {
			LOGGER().error("Could not send EoG Custom Message.");
		}
	}

	protected void sendWordcloud() {
		try {
			new WordcloudCommand(nhlBot).sendWordcloud(channel, guild, game);
		} catch (Exception e) {
			LOGGER().error("Could not send Wordcloud.");
		}
	}
}
