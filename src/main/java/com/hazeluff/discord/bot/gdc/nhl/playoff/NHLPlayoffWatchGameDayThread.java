package com.hazeluff.discord.bot.gdc.nhl.playoff;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.gdc.GDCScoreCommand;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMeta;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.bot.gdc.GameDayThread;
import com.hazeluff.discord.bot.gdc.nhl.NHLGameDayThread;
import com.hazeluff.discord.nhl.NHLGameTracker;
import com.hazeluff.nhl.game.NHLGame;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.ThreadChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.StartThreadFromMessageRequest;

public class NHLPlayoffWatchGameDayThread extends NHLGameDayThread {
	private static final Logger LOGGER = LoggerFactory.getLogger(NHLPlayoffWatchGameDayThread.class);

	@Override
	protected Logger LOGGER() {
		return LOGGER;
	}

	private NHLPlayoffWatchGameDayThread(NHLBot nhlBot, NHLGameTracker gameTracker, Guild guild, MessageChannel channel,
		MessageChannel parentChannel, GDCMeta meta) {
		super(nhlBot, gameTracker, guild, channel, parentChannel, meta, true);
	}

	public static NHLPlayoffWatchGameDayThread getOrCreate(NHLBot nhlBot, MessageChannel messageChannel,
		NHLGameTracker gameTracker, Guild guild, boolean useThreads) {
		long guildId = guild.getId().asLong();
		int gameId = gameTracker.getGame().getGameId();

		GDCMeta meta = null;

		if (messageChannel == null) {
			LOGGER.warn(
				"messageChannel is null (no parent for Thread/no Channel). GameDayChannel not started. guild={}",
				guildId);
			return new NHLPlayoffWatchGameDayThread(nhlBot, gameTracker, guild, messageChannel, messageChannel, meta);
		}

		MessageChannel parentChannel = messageChannel;
		long parentChannelId = parentChannel.getId().asLong();

		// GDC in threads
		if (useThreads) {
			messageChannel = null;
			meta = nhlBot.getPersistentData().getGDCMetaData().loadMetaByParentId(parentChannelId, gameId);

			// Try to get existing Thread (Channel)
			if (meta != null) {
				messageChannel = DiscordManager.getMessageChannel(guild, meta.getChannelId());
			}

			// Create new Thread (Channel) if none found/exist.
			if (messageChannel == null) {
				meta = null; // Generate new meta
				NHLGame game = gameTracker.getGame();
				String threadMsg = "Game Day Thread: " + GameDayThread.buildDetailsMessage(game);
				Message message = DiscordManager.sendAndGetMessage(parentChannel, threadMsg);
				if (message != null) {
					StartThreadFromMessageRequest request = StartThreadFromMessageRequest.builder()
						.name(GameDayThread.buildThreadTitle(game))
						.build();
					ThreadChannel threadChannel = DiscordManager.block(message.createPublicThread(request));
					if (threadChannel != null) {
						messageChannel = threadChannel;
					}
				}
			}
		} else {
			meta = nhlBot.getPersistentData().getGDCMetaData().loadMetaByChannelId(parentChannelId, gameId);
			// WatchThread will be made from messageChannel
		}

		// Channel exists; No meta exists.
		if (messageChannel != null && meta == null) {
			// Generate new metadata
			if (useThreads)
				meta = GDCMeta.forThread(messageChannel.getId().asLong(), gameId, parentChannelId);
			else
				meta = GDCMeta.forChannel(messageChannel.getId().asLong(), gameId);
			nhlBot.getPersistentData().getGDCMetaData().save(meta);
		}

		// Make and return WatchThread
		NHLPlayoffWatchGameDayThread gdt = new NHLPlayoffWatchGameDayThread(nhlBot, gameTracker, guild, messageChannel,
			parentChannel, meta);
		if (gdt.threadChannel != null) {
			gdt.start();
		} else {
			LOGGER.warn("GameDayChannel not started. `messageChannel` was null. guild={}", guildId);
		}
		return gdt;
	}

	@Override
	protected void initChannel() {
		loadMetadata();
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
	}

	@Override
	protected String buildReminderMessage(String basicMessage) {
		return getMatchupName() + ": " + basicMessage;
	}

	@Override
	protected Map<Long, String> getReminders() {
		return new HashMap<Long, String>() {
			{
				put(3600000l, "60 minutes till puck drop.");
			}
		};
	}

	@Override
	protected String buildStartOfGameMessage() {
		String message = String.format("%s\n", getMatchupName());
		message += "Game is about to start!";
		return message;
	}

	@Override
	protected EmbedCreateSpec getSummaryEmbedSpec() {
		EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();
		embedBuilder.addField("NHL Playoffs", buildDetailsMessage(game), false);
		GDCScoreCommand.buildEmbed(embedBuilder, game);
		return embedBuilder.build();
	}

	/**
	 * Gets the message that NHLBot will respond with when queried about this game
	 * 
	 * @param game
	 *            the game to get the message for
	 * @param timeZone
	 *            the time zone to localize to
	 * 
	 * @return message in the format: "The next game is:\n<br>
	 *         **Home Team** vs **Away Team** at HH:mm aaa on EEEE dd MMM yyyy"
	 */
	public static String buildDetailsMessage(NHLGame game) {
		String time = game.isStartTimeTBD()
				? "`TBD`"
				: String.format("<t:%s>", game.getStartTime().toEpochSecond());
		String message = String.format(
				"**%s** vs **%s** at %s", 
				game.getHomeTeam().getLocationName(), game.getAwayTeam().getLocationName(), 
				time
			);
		return message;
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
		String message = getMatchupName();
		message += "\nGame has ended.\n" + "Final Score: " + buildGameScore(game);
		return message;
	}
}
