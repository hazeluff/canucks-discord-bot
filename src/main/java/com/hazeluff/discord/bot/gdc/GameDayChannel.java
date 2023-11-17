package com.hazeluff.discord.bot.gdc;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.bot.listener.IEventProcessor;
import com.hazeluff.discord.nhl.GameTracker;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.Team;
import com.hazeluff.nhl.game.BoxScoreData.TeamStats;
import com.hazeluff.nhl.game.Game;

import discord4j.core.event.domain.Event;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;

public class GameDayChannel extends Thread implements IEventProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameDayChannel.class);

	// Number of retries to do when NHL API returns no events.
	static final int NHL_EVENTS_RETRIES = 5;

	static final long NOT_STARTED_POLL_RATE_MS = 600000l; // 10m
	static final long ACTIVE_POLL_RATE_MS = 30000l; // 30s
	static final int REMINDER_THRESHOLD_MIN = 5;


	private final NHLBot nhlBot;
	@SuppressWarnings("unused")
	private final GameTracker gameTracker;
	private final Game game;
	private final Guild guild;
	private final TextChannel channel;

	private AtomicBoolean started = new AtomicBoolean(false);

	private GameDayChannel(NHLBot nhlBot, GameTracker gameTracker, Guild guild, TextChannel channel) {
		this.nhlBot = nhlBot;
		this.gameTracker = gameTracker;
		this.game = gameTracker.getGame();
		this.guild = guild;
		this.channel = channel;
	}

	public static GameDayChannel get(NHLBot nhlBot, GameTracker gameTracker, Guild guild, TextChannel textChannel) {
		GameDayChannel gameDayChannel = new GameDayChannel(nhlBot, gameTracker, guild, textChannel);
		gameDayChannel.start();
		return gameDayChannel;
	}

	/*
	 * Thread
	 */

	@Override
	public void start() {
		if (started.compareAndSet(false, true)) {
			superStart();
		} else {
			LOGGER.warn("Thread already started.");
		}
	}

	void superStart() {
		super.start();
	}

	@Override
	public void run() {
		try {
			_run();
		} catch (Exception e) {
			LOGGER.error("Error occurred while running thread.", e);
		} finally {
			LOGGER.info("Thread completed");
		}
	}

	private void _run() {
		String channelName = getChannelName(this.game);
		String threadName = String.format("<%s> <%s>", guild.getName(), channelName);
		setName(threadName);
		LOGGER.info("Started GameDayChannel thread.");
		if (!game.getGameState().isFinished()) {
			LOGGER.info("Waiting for game to start.");
			while (!game.getGameState().isStarted()) {
				Utils.sleep(NOT_STARTED_POLL_RATE_MS);
			}
			LOGGER.info("Game has started.");
			while (!game.getGameState().isFinished()) {
				try {
					Utils.sleep(ACTIVE_POLL_RATE_MS);
					if (game.getPeriod() == 3 && (game.getClockRemainingSeconds() < REMINDER_THRESHOLD_MIN * 60)) {
						sendReminder();
						break;
					}
				} catch (Exception e) {
					LOGGER.error("Exception occured while running.", e);
				}
			}
		} else {
			LOGGER.info("Game is already finished");
		}

	}

	void sendReminder() {
		LOGGER.info("Sending Reminder.");
		String message = String.format("**%s** (%s) vs **%s** (%s) - %s minutes left",
				game.getHomeTeam().getCode(), game.getHomeScore(),
				game.getAwayTeam().getCode(), game.getAwayScore(),
				REMINDER_THRESHOLD_MIN);
		sendMessage(message, buildEmbed(game));
	}

	public static EmbedCreateSpec buildEmbed(Game game) {
		EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();
		TeamStats homeStats = game.getHomeStats();
		String homeGoals = String.format("Goals: **%s**", game.getHomeScore());
		String homeSog = String.format("SOG: **%s**", homeStats.getSog());
		String homePp = String.format("PP: **%s**", homeStats.getPowerPlayConv());
		String homeFo = String.format("FO: **%.1f%%**", homeStats.getFaceoffWinPctg());
		String homeHit = String.format("Hits: **%s**", homeStats.getHits());
		String homeBlock = String.format("Blocks: **%s**", homeStats.getBlocks());
		String homePim = String.format("PIM: **%s**", homeStats.getPim());
		embedBuilder.addField(
			game.getHomeTeam().getFullName(),
			"Home"
			+ "\n" + homeGoals
			+ "\n" + homeSog
			+ "\n"
			+ "\n" + homePp
			+ "\n" + homeFo
			+ "\n" + homeHit
			+ "\n" + homeBlock
			+ "\n" + homePim,
			true
		);
		embedBuilder.addField(
			"vs",
			"~~", // For formatting
			true
		);
		TeamStats awayStats = game.getAwayStats();
		String awayGoals = "Goals: **" + game.getAwayScore() + "**";
		String awaySog = String.format("SOG: **%s**", awayStats.getSog());
		String awayPp = String.format("PP: **%s**", awayStats.getPowerPlayConv());
		String awayFo = String.format("FO: **%.1f%%**", awayStats.getFaceoffWinPctg());
		String awayHit = String.format("Hits: **%s**", awayStats.getHits());
		String awayBlock = String.format("Blocks: **%s**", awayStats.getBlocks());
		String awayPim = String.format("PIM: **%s**", awayStats.getPim());
		embedBuilder.addField(
			game.getAwayTeam().getFullName(),
			"Away"
			+ "\n" + awayGoals
			+ "\n" + awaySog
			+ "\n"
			+ "\n" + awayPp
			+ "\n" + awayFo
			+ "\n" + awayHit
			+ "\n" + awayBlock
			+ "\n" + awayPim,
			true
		);
		return embedBuilder.build();
	}

	/*
	 * Discord Convenience Methods
	 */
	protected void sendMessage(String message) {
		if (channel != null) {
			DiscordManager.sendMessage(channel, message);
		}
	}

	protected void sendMessage(String message, EmbedCreateSpec embedSpec) {
		MessageCreateSpec messageSpec = MessageCreateSpec.builder()
				.content(message)
				.addEmbed(embedSpec)
				.build();
		if (channel != null) {
			DiscordManager.sendMessage(channel, messageSpec);
		}
	}

	@Override
	public void process(Event event) {
		// Do Nothing
	}

	boolean isBotSelf(User user) {
		return user.getId().equals(nhlBot.getDiscordManager().getId());
	}

	/*
	 * Getters
	 */
	public Guild getGuild() {
		return guild;
	}

	public Game getGame() {
		return game;
	}

	/**
	 * Gets the date in the format "YY-MM-DD"
	 * 
	 * @param zone
	 *            time zone to convert the time to
	 * @return the date in the format "YY-MM-DD"
	 */
	public String getShortDate(ZoneId zone) {
		return getShortDate(game, zone);
	}

	/**
	 * Gets the date in the format "YY-MM-DD"
	 * 
	 * @param game
	 *            game to get the date from
	 * @param zone
	 *            time zone to convert the time to
	 * @return the date in the format "YY-MM-DD"
	 */
	public static String getShortDate(Game game, ZoneId zone) {
		return game.getStartTime().withZoneSameInstant(zone).format(DateTimeFormatter.ofPattern("yy-MM-dd"));
	}

	/**
	 * Gets the date in the format "EEEE dd MMM yyyy"
	 * 
	 * @param game
	 *            game to get the date for
	 * @param zone
	 *            time zone to convert the time to
	 * @return the date in the format "EEEE dd MMM yyyy"
	 */
	public static String getNiceDate(Game game, ZoneId zone) {
		return game.getStartTime().withZoneSameInstant(zone).format(DateTimeFormatter.ofPattern("EEEE, d/MMM/yyyy"));
	}

	/**
	 * Gets the date in the format "EEEE dd MMM yyyy"
	 * 
	 * @param zone
	 *            time zone to convert the time to
	 * @return the date in the format "EEEE dd MMM yyyy"
	 */
	public String getNiceDate(ZoneId zone) {
		return getNiceDate(game, zone);
	}

	/**
	 * Gets the time in the format "HH:mm aaa"
	 * 
	 * @param game
	 *            game to get the time from
	 * @param zone
	 *            time zone to convert the time to
	 * @return the time in the format "HH:mm aaa"
	 */
	public static String getTime(Game game, ZoneId zone) {
		return game.getStartTime().withZoneSameInstant(zone).format(DateTimeFormatter.ofPattern("H:mm z"));
	}

	/**
	 * Gets the time in the format "HH:mm aaa"
	 * 
	 * @param zone
	 *            time zone to convert the time to
	 * @return the time in the format "HH:mm aaa"
	 */
	public String getTime(ZoneId zone) {
		return getTime(game, zone);
	}

	/**
	 * Gets the name that a channel in Discord related to this game would have.
	 * 
	 * @param game
	 *            game to get channel name for
	 * @return channel name in format: "AAA-vs-BBB-yy-MM-DD". <br>
	 *         AAA is the 3 letter code of home team<br>
	 *         BBB is the 3 letter code of away team<br>
	 *         yy-MM-DD is a date format
	 */
	public static String getChannelName(Game game) {
		String channelName = String.format("%.3s-vs-%.3s-%s", game.getHomeTeam().getCode(),
				game.getAwayTeam().getCode(), getShortDate(game, ZoneId.of("America/New_York")));
		return channelName.toLowerCase();

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
		String message = String.format("**%s** vs **%s** at <t:%s>", 
				game.getHomeTeam().getFullName(),
				game.getAwayTeam().getFullName(), 
				game.getStartTime().toEpochSecond());
		return message;
	}

	/**
	 * Determines if the given channel name is that of a possible game. Does not
	 * factor into account whether or not the game is real.
	 * 
	 * @param channelName
	 *            name of the channel
	 * @return true, if is of game channel format;<br>
	 *         false, otherwise.
	 */
	public static boolean isChannelNameFormat(String channelName) {
		String teamRegex = String.join("|", Arrays.asList(Team.values()).stream()
				.map(team -> team.getCode().toLowerCase()).collect(Collectors.toList()));
		teamRegex = String.format("(%s)", teamRegex);
		String regex = String.format("%1$s-vs-%1$s-[0-9]{2}-[0-9]{2}-[0-9]{2}", teamRegex);
		return channelName.matches(regex);
	}

	/*
	 * Thread Management
	 */
	/**
	 * Stops the thread and deletes the channel from the Discord Guild.
	 */
	void stopThread() {
		interrupt();
	}

	@Override
	public void interrupt() {
		super.interrupt();
	}
}
