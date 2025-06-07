package com.hazeluff.discord.bot.gdc.ahl;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.ahl.game.Game;
import com.hazeluff.ahl.game.event.GoalEvent;
import com.hazeluff.ahl.game.event.Player;
import com.hazeluff.discord.ahl.AHLGameTracker;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMeta;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.bot.gdc.GameDayThread;
import com.hazeluff.discord.utils.DateUtils;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;

public class AHLGameDayThread extends GameDayThread {
	private static final Logger LOGGER = LoggerFactory.getLogger(AHLGameDayThread.class);

	@Override
	protected Logger LOGGER() {
		return LOGGER;
	}

	protected final AHLGameTracker gameTracker;
	protected final Game game;

	// Message Managers
	protected final GoalMessagesManager goalMessages;
	protected final PenaltyMessagesManager penaltyMessages;
	protected final ShootoutMessagesManager shootoutMessages;

	private AHLGameDayThread(NHLBot nhlBot, AHLGameTracker gameTracker, Guild guild, TextChannel channel,
			GuildPreferences preferences, GDCMeta meta) {
		super(nhlBot, gameTracker, guild, channel, preferences, meta);
		this.gameTracker = gameTracker;
		this.game = gameTracker.getGame();

		this.goalMessages = new GoalMessagesManager(nhlBot, game, channel, meta);
		this.penaltyMessages = new PenaltyMessagesManager(nhlBot, game, channel, meta);
		this.shootoutMessages = new ShootoutMessagesManager(nhlBot, game, channel);
	}

	protected String buildGameScore(Game game) {
		return String.format("%s **%s** - **%s** %s",
				game.getHomeTeam().getLocationName(), game.getHomeScore(),
				game.getAwayScore(), game.getAwayTeam().getLocationName());
	}

	public static AHLGameDayThread get(NHLBot nhlBot, TextChannel textChannel, AHLGameTracker gameTracker, Guild guild) {
		GuildPreferences preferences = nhlBot.getPersistentData().getPreferencesData()
				.getGuildPreferences(guild.getId().asLong());
		GDCMeta meta = null;
		if (textChannel != null) {
			meta = nhlBot.getPersistentData().getGDCMetaData().loadMeta(
				textChannel.getId().asLong(),
				gameTracker.getGame().getId()
			);
			if (meta == null) {
				meta = GDCMeta.of(textChannel.getId().asLong(), gameTracker.getGame().getId());
			}
		}
		AHLGameDayThread gdt = new AHLGameDayThread(nhlBot, gameTracker, guild,
				textChannel, preferences, meta);

		// gdt.updateMessages(); // Uncomment to force messages to be sent (for testing)

		if (gdt.channel != null) {
			gdt.start();
		} else {
			LOGGER.warn("GameDayChannel not started. TextChannel could not be found. guild={}", guild.getId().asLong());
		}
		return gdt;
	}

	@Override
	protected void setThreadName() {
		setName(game.getNiceName());
	}

	@Override
	protected long timeUntilGame() {
		ZonedDateTime startTime = game.getStartTime();
		if (startTime == null) {
			return Long.MAX_VALUE;
		}
		return DateUtils.diffMs(ZonedDateTime.now(), startTime);
	}

	@Override
	protected void initChannel() {
		loadMetadata();
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

	protected void updateMessages() {
		try {
			goalMessages.updateMessages(game.getGoalEvents());
			penaltyMessages.updateMessages(game.getPenaltyEvents());
			shootoutMessages.updateMessages(game.getShootoutEvents());
		} catch (Exception e) {
			LOGGER().error("Exception occured while updating messages.", e);
		}
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

	/*
	 * Metadata
	 */
	protected void loadMetadata() {
		LOGGER().trace("Load Metadata.");
		// Load Goal Messages
		this.goalMessages.initEventMessages(meta.getGoalMessageIds());
		this.goalMessages.initEvents(game.getGoalEvents());
		// Load Penalty Messages
		this.penaltyMessages.initEventMessages(meta.getPenaltyMessageIds());
		this.penaltyMessages.initEvents(game.getPenaltyEvents());
		// Load Shootout Messages
		this.shootoutMessages.initEvents(game.getShootoutEvents());
		saveMetadata();
	}

	private void saveMetadata() {
		nhlBot.getPersistentData().getGDCMetaData().save(meta);
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

	private Message sendSummaryMessage() {
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
		embedBuilder.addField(getMatchupName(), game.getNiceDate(), false);
		appendScoreToEmbed(embedBuilder);
		appendGoalsToEmbed(embedBuilder);
		return embedBuilder.build();
	}

	protected EmbedCreateSpec.Builder appendScoreToEmbed(EmbedCreateSpec.Builder embedBuilder) {
		String homeTeam = game.getHomeTeam().getFullName();
		String awayTeam = game.getAwayTeam().getFullName();
		return embedBuilder
				.addField(
						homeTeam,
						"Home: " + " " + game.getHomeScore(),
						true
				)
				.addField(
						"vs",
						"᲼᲼", // For formatting
						true
				)
				.addField(
						awayTeam,
						"Away: " + game.getAwayScore(),
						true
				);
	}

	public EmbedCreateSpec.Builder appendGoalsToEmbed(EmbedCreateSpec.Builder embedBuilder) {
		List<GoalEvent> goals = game.getGoalEvents();
		// Regulation Periods
		for (int period = 1; period <= 3; period++) {
			String strPeriod = ""; // Field Title
			switch (period) {
			case 1:
				strPeriod = "1st Period";
				break;
			case 2:
				strPeriod = "2nd Period";
				break;
			case 3:
				strPeriod = "3rd Period";
				break;
			}
			String strGoals = ""; // Field Body
			int fPeriod = period;
			Predicate<GoalEvent> isPeriod = gameEvent -> gameEvent.getGoalDetails().getPeriod() == fPeriod;
			if (goals.stream().anyMatch(isPeriod)) {
				List<String> strPeriodGoals = goals.stream()
						.filter(isPeriod)
						.map(goalEvent -> buildGoalLine(goalEvent))
						.collect(Collectors.toList());
				strGoals = String.join("\n", strPeriodGoals);
			} else {
				strGoals = "None";
			}
			embedBuilder.addField(strPeriod, strGoals, false);
		}
		// Overtime
		Predicate<GoalEvent> isExtraPeriod = gameEvent -> gameEvent.getGoalDetails().getPeriod() > 3;
		if (goals.stream().anyMatch(isExtraPeriod)) {
			GoalEvent otGoal = goals.stream().filter(isExtraPeriod).findAny().orElse(null);
			String periodName = otGoal.getGoalDetails().getPeriodLongName();
			String strGoal = buildGoalLine(otGoal);
			embedBuilder.addField(periodName, strGoal, false);
		}

		String status = game.getStatus();
		embedBuilder.footer("Status: " + status, null);
		return embedBuilder;
	}
	
	private static String buildGoalLine(GoalEvent goalEvent) {
		StringBuilder details = new StringBuilder();
		Player scorer = goalEvent.getGoalDetails().getScorer();
		List<Player> assists = goalEvent.getGoalDetails().getAssists();
		details.append(String.format("**%s** @ %s - **%-18s**", 
				goalEvent.getGoalDetails().getTeam().getTeamCode(), goalEvent.getGoalDetails().getTime(), scorer.getFullName()));
		if (assists.size() > 0) {
			details.append("  Assists: ");
			details.append(assists.get(0).getFullName());
		}
		if (assists.size() > 1) {
			details.append(", ");
			details.append(assists.get(1).getFullName());
		}
		
		appendSpecialDetails(details, goalEvent);

		return details.toString();
	}

	private static void appendSpecialDetails(StringBuilder details, GoalEvent goalEvent) {
		List<Object[]> specialDetails = Arrays.asList(
			new Object[] { goalEvent.getGoalDetails().isPowerPlay(), "Power Play" },
			new Object[] { goalEvent.getGoalDetails().isShortHanded(), "Short Handed" },
			new Object[] { goalEvent.getGoalDetails().isPenaltyShot(), "Penalty Shot" },
			new Object[] { goalEvent.getGoalDetails().isEmptyNet(), "Empty Net"}	
		);
		boolean isAnyDetails = specialDetails.stream().anyMatch(detail -> (boolean) detail[0]);
		if (isAnyDetails) {
			details.append(" (");
			boolean isFirstMatch = true;
			for (Object[] detail : specialDetails) {
				if ((boolean) detail[0]) {
					if (isFirstMatch) {
						isFirstMatch = false;
					} else {
						details.append("; ");
					}
					details.append(detail[1]);
				}
			}
			details.append(")");
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
	protected String buildEndOfGameMessage() {
		String message = getMatchupName();
		message += "\nGame has ended.\n" + "Final Score: " + buildGameScore(game);
		return message;
	}

	String getMatchupName() {
		return buildMatchupName(game);
	}

	public static String buildMatchupName(Game game) {
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
		String time = String.format("<t:%s>", game.getStartTime().toEpochSecond());
		String message = String.format(
				"**%s** vs **%s** at %s", 
				game.getHomeTeam().getLocationName(), game.getAwayTeam().getLocationName(), 
				time
			);
		return message;
	}
}
