package com.hazeluff.discord.bot.gdc.playoff;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.gdc.GDCScoreCommand;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMeta;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.bot.gdc.GameDayChannel;
import com.hazeluff.discord.nhl.GameTracker;
import com.hazeluff.nhl.game.Game;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;

public class PlayoffWatchGameDayThread extends GameDayChannel {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlayoffWatchGameDayThread.class);

	@Override
	protected Logger LOGGER() {
		return LOGGER;
	}

	private PlayoffWatchGameDayThread(NHLBot nhlBot, GameTracker gameTracker, Guild guild, TextChannel channel,
			GuildPreferences preferences, GDCMeta meta) {
		super(nhlBot, gameTracker, guild, channel, preferences, meta);
	}

	public static PlayoffWatchGameDayThread get(NHLBot nhlBot, TextChannel textChannel, GameTracker gameTracker, Guild guild) {
		GuildPreferences preferences = nhlBot.getPersistentData().getPreferencesData()
				.getGuildPreferences(guild.getId().asLong());
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
		PlayoffWatchGameDayThread gdt = new PlayoffWatchGameDayThread(nhlBot, gameTracker, guild,
				textChannel, preferences, meta);
		if (gdt.channel != null) {
			gdt.start();
		} else {
			LOGGER.warn("GameDayChannel not started. TextChannel could not be found. guild={}", guild.getId().asLong());
		}
		return gdt;
	}

	@Override
	protected void initChannel() {
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
	}

	public void unpinSummaryMessage() {
		if (summaryMessage != null) {
			DiscordManager.unpinMessage(summaryMessage);
		}
	}

	@Override
	protected void updateOnReminderWait() {
		initSummaryMessage();
		updateSummaryMessage();
	}

	@Override
	protected String buildReminderMessage(String basicMessage) {
		return getMatchupName() + ": " + basicMessage;
	}

	@SuppressWarnings("serial")
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
		String message = String.format("%s: \n", getMatchupName());
		message += "Game is about to start!";
		return message;
	}

	@Override
	protected EmbedCreateSpec getSummaryEmbedSpec() {
		EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();
		embedBuilder.addField("NHL Playoffs", buildDetailsMessage(getGame()), false);
		GDCScoreCommand.buildEmbed(embedBuilder, game);
		return embedBuilder.build();
	}

	/*
	 * End of game message
	 */
	/**
	 * Sends the end of game message.
	 */
	@Override
	protected void sendEndOfGameMessage() {
		try {
			if (channel != null) {
				DiscordManager.sendAndGetMessage(channel, buildEndOfGameMessage());
			}
		} catch (Exception e) {
			LOGGER.error("Could not send end of game Message.");
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
		String message = getMatchupName();
		message += "\nGame has ended.\n" + "Final Score: " + buildGameScore(game);
		return message;
	}

	String getMatchupName() {
		return buildMatchupName(getGame());
	}

	public static String buildMatchupName(Game game) {
		return String.format(
				"**%s** vs **%s**", 
				game.getHomeTeam().getLocation(), game.getAwayTeam().getLocation()
			);
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
	public static String buildDetailsMessage(Game game) {
		String time = game.isStartTimeTBD()
				? "`TBD`"
				: String.format("<t:%s>", game.getStartTime().toEpochSecond());
		String message = String.format(
				"**%s** vs **%s** at %s", 
				game.getHomeTeam().getLocation(), game.getAwayTeam().getLocation(), 
				time
			);
		return message;
	}
}
