package com.hazeluff.discord.bot.gdc.nhl.fournations;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.gdc.GDCGoalsCommand;
import com.hazeluff.discord.bot.command.gdc.GDCScoreCommand;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMeta;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.gdc.nhl.NHLGameDayThread;
import com.hazeluff.discord.nhl.NHLGameTracker;
import com.hazeluff.nhl.game.Game;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;

public class FourNationsGameDayThread extends NHLGameDayThread {
	private static final Logger LOGGER = LoggerFactory.getLogger(FourNationsGameDayThread.class);

	@Override
	protected Logger LOGGER() {
		return LOGGER;
	}

	private FourNationsGameDayThread(NHLBot nhlBot, NHLGameTracker gameTracker, Guild guild, TextChannel channel,
			GuildPreferences preferences, GDCMeta meta) {
		super(nhlBot, gameTracker, guild, channel, preferences, meta);
	}

	public static FourNationsGameDayThread get(NHLBot nhlBot, TextChannel textChannel, NHLGameTracker gameTracker, Guild guild) {
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
		FourNationsGameDayThread gameDayChannel = new FourNationsGameDayThread(nhlBot, gameTracker, guild,
				textChannel, preferences, meta);

		if (gameDayChannel.channel != null) {
			gameDayChannel.start();
		} else {
			LOGGER.warn("GameDayChannel not started. TextChannel could not be found. guild={}", guild.getId().asLong());
		}
		return gameDayChannel;
	}

	@Override
	protected void initChannel() {
		initSummaryMessage();
		updateSummaryMessage();
	}

	@Override
	protected void updateActive() {
		updateMessages();
	}

	@Override
	protected void updateEnd() {
		sendEndOfGameMessage();
	}

	@Override
	protected String buildReminderMessage(String basicMessage) {
		return getFourNationsMatchupName() + ": " + basicMessage;
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
		String message = String.format("%s: \n", getFourNationsMatchupName());
		message += "Game is about to start!";
		return message;
	}

	@Override
	protected EmbedCreateSpec getSummaryEmbedSpec() {
		EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();
		embedBuilder.addField("Four Nations", buildDetailsMessage(game), false);
		GDCScoreCommand.buildEmbed(embedBuilder, game);
		GDCGoalsCommand.buildEmbed(embedBuilder, game);
		return embedBuilder.build();
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
		String message = getFourNationsMatchupName();
		message += "\nGame has ended.\n" + "Final Score: " + buildGameScore(game);
		return message;
	}

	String getFourNationsMatchupName() {
		return buildFourNationsMatchupName(game);
	}

	public static String buildFourNationsMatchupName(Game game) {
		return String.format(
				"**%s** vs **%s**", 
				game.getHomeTeam().getLocationName(), game.getAwayTeam().getLocationName()
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
				game.getHomeTeam().getLocationName(), game.getAwayTeam().getLocationName(), 
				time
			);
		return message;
	}
}
