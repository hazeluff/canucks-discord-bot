package com.hazeluff.discord.bot.gdc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.channel.ChannelMessage;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.nhl.GameTracker;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.Team;
import com.hazeluff.nhl.game.Game;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;

/**
 * This class is used to manage the channels in a Guild.
 */
public class GameDayChannelsManager extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameDayChannelsManager.class);

	// Poll for every 5 seconds, (On initialization)
	static final long INIT_UPDATE_RATE = 5000L;
	// Poll for every 5 minutes - if the scheduler has updated
	static final long UPDATE_RATE = 300000L;

	private final NHLBot nhlBot;
	// Map<GuildId, ScheduleMessage>
	private Map<Long, Message> scheduleMessages;
	// Map<GuildId, Map<GameId, GameDayChannel>>
	private final Map<Long, Map<Integer, GameDayChannel>> gameDayChannels;

	public GameDayChannel getGameDayChannel(long guildId, int gameId) {
		if (!gameDayChannels.containsKey(guildId)) {
			return null;
		}

		if (!gameDayChannels.get(guildId).containsKey(gameId)) {
			return null;
		}

		return gameDayChannels.get(guildId).get(gameId);
	}

	public Map<Integer, GameDayChannel> getGameDayChannels(long guildId) {
		if (!gameDayChannels.containsKey(guildId)) {
			return Collections.emptyMap();
		}
		return gameDayChannels.get(guildId);
	}

	boolean isGameDayChannelExist(long guildId, int gameId) {
		return getGameDayChannel(guildId, gameId) != null;
	}

	void addGameDayChannel(long guildId, int gameId, GameDayChannel gameDayChannel) {
		if (!gameDayChannels.containsKey(guildId)) {
			gameDayChannels.put(guildId, new ConcurrentHashMap<>());
		}
		gameDayChannels.get(guildId).put(gameId, gameDayChannel);
	}

	/**
	 * 
	 * @param guildId
	 * @param gameId
	 * @return the removed GameDayChannel
	 */
	GameDayChannel removeGameDayChannel(long guildId, int gameId) {
		if (!gameDayChannels.containsKey(guildId)) {
			return null;
		}

		Map<Integer, GameDayChannel> guildChannels = gameDayChannels.get(guildId);
		GameDayChannel gameDayChannel = guildChannels.remove(gameId);
		if (gameDayChannel == null) {
			return null;
		}

		gameDayChannel.stopThread();

		if (guildChannels.isEmpty()) {
			gameDayChannels.remove(guildId);
		}
		return gameDayChannel;
	}

	public GameDayChannelsManager(NHLBot nhlBot) {
		this.nhlBot = nhlBot;
		this.scheduleMessages = new ConcurrentHashMap<>();
		this.gameDayChannels = new ConcurrentHashMap<>();
	}

	@Override
	public void run() {
		LOGGER.info("GameDayChannelsManager Thread started.");
		initScheduleMessages();
		LocalDate lastUpdate = null;
		while (!isStop()) {
			try {
				LocalDate schedulerUpdate = nhlBot.getGameScheduler().getLastUpdate();
				if (schedulerUpdate == null) {
					LOGGER.info("Waiting for GameScheduler to initialize...");
					Utils.sleep(INIT_UPDATE_RATE);
				} else if (lastUpdate == null || schedulerUpdate.compareTo(lastUpdate) > 0) {
					LOGGER.info("Updating Channels...");
					updateGuilds();
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

	void initScheduleMessages() {
		for (Guild guild : nhlBot.getDiscordManager().getGuilds()) {
			getOrCreateScheduleMessage(guild);
		}
	}

	Message getOrCreateScheduleMessage(Guild guild) {
		long guildId = guild.getId().asLong();
		if (scheduleMessages.containsKey(guildId)) {
			return scheduleMessages.get(guildId);
		}
		TextChannel textChannel = nhlBot.getNHLReminderChannel(guild);
		long channelId = textChannel.getId().asLong();
		ChannelMessage savedMessage = nhlBot.getPersistentData().getChannelMessagesData().loadMessage(channelId, "schedule");
		if (savedMessage != null) {
			return nhlBot.getDiscordManager().getMessage(channelId, savedMessage.getMessageId());
		} else {
			List<Team> teams = nhlBot
					.getPersistentData()
					.getPreferencesData()
					.getGuildPreferences(guild.getId().asLong())
					.getTeams();
			List<Game> activeGames = nhlBot.getGameScheduler().getActiveGames(teams);
			Message message = DiscordManager.sendAndGetMessage(textChannel, buildScheduleMessageSpec(activeGames));
			if(message != null) {
				DiscordManager.pinMessage(message);
				nhlBot.getPersistentData().getChannelMessagesData().saveMessage(
						ChannelMessage.of(textChannel.getId().asLong(), message.getId().asLong(), "schedule")
				);
				scheduleMessages.put(guildId, message);
			}
			return message;
		}
	}

	public void updateScheduleMessage(Guild guild, List<Game> games) {
		Message scheduleMessage = getOrCreateScheduleMessage(guild);
		if (scheduleMessage != null) {
			MessageEditSpec messageEditSpec = MessageEditSpec.builder().addEmbed(buildEmbedCreateSpec(games)).build();
			DiscordManager.updateMessage(scheduleMessage, messageEditSpec);
		} else {
			LOGGER.error("Failed to get message for: " + guild.getId().asLong());
		}
	}

	MessageCreateSpec buildScheduleMessageSpec(List<Game> games) {
		return MessageCreateSpec.builder().addEmbed(buildEmbedCreateSpec(games)).build();
	}

	EmbedCreateSpec buildEmbedCreateSpec(List<Game> games) {
		EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
		builder.description("Schedule for upcoming games. (Updated Daily)");
		games.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));
		for (Game game : games) {
			String gameName = String.format("**%s** vs **%s**", game.getHomeTeam().getName(),
					game.getAwayTeam().getName());
			long ts = game.getStartTime().toEpochSecond();
			String timeTill = String.format("<t:%1$s> (<t:%1$s:R>)", ts);
			builder.addField(gameName, timeTill, false);
		}
		return builder.build();
	}

	/**
	 * Gets the guilds that are subscribed to the specified team.
	 * 
	 * @param team
	 *            team that the guilds are subscribed to
	 * @return list of IGuilds
	 */
	public List<Guild> getSubscribedGuilds(Team team) {
		return nhlBot.getDiscordManager().getGuilds().stream().filter(guild -> {
			long guildId = guild.getId().asLong();
			return nhlBot.getPersistentData().getPreferencesData().getGuildPreferences(guildId).getTeams()
					.contains(team);
		}).collect(Collectors.toList());
	}

	/**
	 * Creates a GameDayChannel for the given game-guild pair. If the channel exist,
	 * the existing one will be returned.
	 * 
	 * @param game
	 * @param guild
	 */
	public GameDayChannel getOrCreateGameDayChannel(Game game, Guild guild) {
		int gameId = game.getGameId();
		long guildId = guild.getId().asLong();
		LOGGER.info("Initializing for channel. channelName={}, guild={}", GameDayChannel.getChannelName(game), guildId);
		TextChannel textChannel = nhlBot.getNHLReminderChannel(guild);
		if (textChannel == null) {
			return null;
		}
		GameDayChannel gameDayChannel = getGameDayChannel(guildId, gameId);
		if (gameDayChannel == null) {
			GameTracker gameTracker = nhlBot.getGameScheduler().getGameTracker(game);
			if (gameTracker != null) {
				gameDayChannel = GameDayChannel.get(nhlBot, gameTracker, guild, textChannel);
				addGameDayChannel(guildId, gameId, gameDayChannel);
			} else {
				LOGGER.error("Could not find GameTracker for game [{}]", game);
			}
		} else {
			LOGGER.debug("Game Day Channel already exists for game [{}] in guild [{}]", gameId, guildId);
		}

		return gameDayChannel;
	}

	public void updateGuilds() {
		for(Guild guild : nhlBot.getDiscordManager().getGuilds()) {
			updateGuild(guild);
		}
	}

	/**
	 * <p>
	 * Creates and deletes channels, to match the current subscribed teams of a
	 * guild.
	 * </p>
	 * 
	 * <p>
	 * Removes the GameDayChannels from the stored Map
	 * </p>
	 * 
	 * @param guild
	 */
	public void updateGuild(Guild guild) {
		try {
			long guildId = guild.getId().asLong();
			List<Team> teams = nhlBot.getPersistentData().getPreferencesData()
					.getGuildPreferences(guildId).getTeams();
			List<Game> activeGames = nhlBot.getGameScheduler().getActiveGames(teams);

			LOGGER.info("Updating Channels for [{}]: activeGames={}", guildId, activeGames);

			// Create game channels of latest game for current subscribed team
			updateScheduleMessage(guild, activeGames);
			for (Game game : activeGames) {
				getOrCreateGameDayChannel(game, guild);
			}
			List<Integer> gameIdsToRemove = getGameDayChannels(guildId).entrySet()
					.stream()
					.filter(entry -> !activeGames.stream()
							.anyMatch(game -> game.getGameId() == entry.getValue().getGame().getGameId())
					)
					.map(entry -> entry.getValue().getGame().getGameId())
					.collect(Collectors.toList());
			for (Integer gameId : gameIdsToRemove) {
				removeGameDayChannel(guildId, gameId);
			}
		} catch (Exception e) {
			LOGGER.warn("Issue updating guild: " + guild.getId().asLong(), e);
		}
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
