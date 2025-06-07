package com.hazeluff.discord.bot.gdc.nhl;

import java.time.ZonedDateTime;
import java.util.List;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.gdc.GDCGoalsCommand;
import com.hazeluff.discord.bot.command.gdc.GDCScoreCommand;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMeta;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.bot.gdc.GameDayThread;
import com.hazeluff.discord.nhl.NHLGameTracker;
import com.hazeluff.discord.utils.DateUtils;
import com.hazeluff.nhl.game.Game;
import com.hazeluff.nhl.game.event.GoalEvent;
import com.hazeluff.nhl.game.event.PenaltyEvent;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.possible.Possible;

public abstract class NHLGameDayThread extends GameDayThread {

	protected final NHLGameTracker gameTracker;
	protected final Game game;

	// Message Managers
	protected final GoalMessagesManager goalMessages;
	protected final PenaltyMessagesManager penaltyMessages;

	public NHLGameDayThread(NHLBot nhlBot, NHLGameTracker gameTracker, Guild guild, TextChannel textChannel,
			GuildPreferences preferences, GDCMeta meta) {
		super(nhlBot, gameTracker, guild, textChannel, preferences, meta);
		this.gameTracker = gameTracker;
		this.game = gameTracker.getGame();

		this.goalMessages = new GoalMessagesManager(nhlBot, game, channel, meta);
		this.penaltyMessages = new PenaltyMessagesManager(nhlBot, game, channel, meta);
	}

	protected String buildGameScore(Game game) {
		return String.format("%s **%s** - **%s** %s", game.getHomeTeam().getName(), game.getHomeScore(),
				game.getAwayScore(), game.getAwayTeam().getName());
	}

	/*
	 * Run method overrides
	 */
	protected void setThreadName() {
		setName(game.getNiceName());
	}

	protected long timeUntilGame() {
		return DateUtils.diffMs(ZonedDateTime.now(), game.getStartTime());
	}

	protected void initChannel() {
		loadMetadata();
	}

	@Override
	protected void waitAndSendReminders() {
		if (game.isStartTimeTBD()) {
			return;
		}
		super.waitAndSendReminders();
	}

	protected void updateMessages() {
		try {
			List<GoalEvent> goalEvents = game.getScoringEvents();
			List<PenaltyEvent> penaltyEvents = game.getPenaltyEvents();
			goalMessages.updateMessages(goalEvents);
			penaltyMessages.updateMessages(penaltyEvents);
		} catch (Exception e) {
			LOGGER().error("Exception occured while updating messages.", e);
		}
	}

	/*
	 * Metadata
	 */
	protected void loadMetadata() {
		LOGGER().trace("Load Metadata.");
		// Load Goal Messages
		this.goalMessages.initEventMessages(meta.getGoalMessageIds());
		this.goalMessages.initEvents(game.getScoringEvents());
		// Load Penalty Messages
		this.penaltyMessages.initEventMessages(meta.getPenaltyMessageIds());
		this.penaltyMessages.initEvents(game.getPenaltyEvents());

		saveMetadata();
	}

	/*
	 * Matchup
	 */
	private void saveMetadata() {
		nhlBot.getPersistentData().getGDCMetaData().save(meta);
	}

	protected String getMatchupName() {
		return buildMatchupName(game);
	}

	public static String buildMatchupName(Game game) {
		return String.format(
				"**%s** vs **%s**", 
				game.getHomeTeam().getLocationName(), game.getAwayTeam().getLocationName()
			);
	}

	/*
	 * Intro Message
	 */
	private Message getIntroMessage() {
		Message message = null;
		if (meta != null) {
			Long messageId = meta.getIntroMessageId();
			if (messageId == null) {
				// No message saved
				message = sendIntroMessage();
			} else {
				message = nhlBot.getDiscordManager().getMessage(channel.getId().asLong(), messageId);
				if (message == null) {
					// Could not find existing message. Send new message
					message = sendIntroMessage();
				} else {
					// Message exists
					return message;
				}
			}

			if (message != null) {
				DiscordManager.pinMessage(message);
				meta.setIntroMessageId(message.getId().asLong());
				saveMetadata();
			}
		}
		return message;
	}

	private Message sendIntroMessage() {
		String strMessage = buildIntroMessage();
		MessageCreateSpec messageSpec = MessageCreateSpec.builder().content(strMessage).build();
		return DiscordManager.sendAndGetMessage(channel, messageSpec);
	}

	public void initIntroMessage() {
		LOGGER().info("Init Intro Message.");
		introMessage = getIntroMessage();
	}

	public void updateIntroMessage() {
		String strMessage = buildIntroMessage();
		MessageEditSpec messageSpec = MessageEditSpec.builder()
				.content(Possible.of(java.util.Optional.ofNullable(strMessage))).build();
		if (introMessage != null) {
			DiscordManager.updateMessage(introMessage, messageSpec);
		}
	}

	private String buildIntroMessage() {
		return buildDetailsMessage(game) + "\n\n" + getHelpMessageText();
	}

	protected static String getHelpMessageText() {
		return "This game/channel is interactable with Slash Commands!"
				+ "\nUse `/gdc subcommand:help` to bring up a list of commands.";
	}

	/*
	 * Summary Message
	 */
	public void initSummaryMessage() {
		if (summaryMessage == null) {
			LOGGER().info("Init Summary Message.");
			summaryMessage = getSummaryMessage();
		}
	}

	private Message getSummaryMessage() {
		Message message = null;
		if (meta != null) {
			Long messageId = meta.getSummaryMessageId();
			if (messageId == null) {
				// No message saved
				message = sendSummaryMessage();
			} else {
				message = nhlBot.getDiscordManager().getMessage(channel.getId().asLong(), messageId);
				if (message == null) {
					// Could not find existing message. Send new message
					message = sendSummaryMessage();
				} else {
					// Message exists
					return message;
				}
			}

			if (message != null) {
				DiscordManager.pinMessage(message);
				meta.setSummaryMessageId(message.getId().asLong());
				saveMetadata();
			}
		}
		return message;
	}

	protected Message sendSummaryMessage() {
		this.summaryMessageEmbed = getSummaryEmbedSpec();
		MessageCreateSpec messageSpec = MessageCreateSpec.builder().addEmbed(summaryMessageEmbed).build();
		return DiscordManager.sendAndGetMessage(channel, messageSpec);
	}

	protected void updateSummaryMessage() {
		EmbedCreateSpec newSummaryMessageEmbed = getSummaryEmbedSpec();
		if (summaryMessage != null && !newSummaryMessageEmbed.equals(summaryMessageEmbed)) {
			this.summaryMessageEmbed = newSummaryMessageEmbed;
			MessageEditSpec messageSpec = MessageEditSpec.builder().addEmbed(summaryMessageEmbed).build();
			DiscordManager.updateMessage(summaryMessage, messageSpec);
		}
	}

	protected EmbedCreateSpec getSummaryEmbedSpec() {
		EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();
		GDCScoreCommand.buildEmbed(embedBuilder, game);
		GDCGoalsCommand.buildEmbed(embedBuilder, game);
		embedBuilder.footer("Status: " + game.getGameState().toString(), null);
		return embedBuilder.build();
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
			LOGGER().error("Could not send Stats Message.");
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
	protected String buildEndOfGameMessage() {
		String message = getMatchupName();
		message += "\nGame has ended.\n" + "Final Score: " + buildGameScore(game);
		return message;
	}

	/*
	 * On Demand Actions
	 */
	/**
	 * Used to update all messages/pins.
	 */
	public void refresh() {
		try {
			gameTracker.updateGame();
			updateMessages();
			updateSummaryMessage();
		} catch (Exception e) {
			LOGGER().error("Exception occured while refreshing.", e);
		}
	}
}
