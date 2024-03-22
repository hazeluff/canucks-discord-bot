package com.hazeluff.discord.bot;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.gdc.GameDayChannel;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.Team;
import com.hazeluff.nhl.game.Game;

import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;

public class PresenceManager extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(PresenceManager.class);

	private static final ClientPresence STARTING_UP_PRESENCE = ClientPresence
			.doNotDisturb(ClientActivity.watching("itself starting up..."));
	private static final ClientPresence FETCH_SCHEDULE_PRESENCE = ClientPresence
			.doNotDisturb(ClientActivity.watching("itself fetch the game schedule..."));

	// Poll for every 5 seconds, (On initialization)
	static final long INIT_UPDATE_RATE = 5000L;
	// Poll for every 5 minutes - if the scheduler has updated
	static final long UPDATE_RATE = 300000L;

	// Config.STATUS_MESSAGE
	private final NHLBot nhlBot;

	public PresenceManager(NHLBot nhlBot) {
		this.nhlBot = nhlBot;
	}

	public void changePresenceToStartup() {
		changePresence(STARTING_UP_PRESENCE);
	}

	private void changePresenceToFetchSchedule() {
		changePresence(FETCH_SCHEDULE_PRESENCE);
	}

	@SuppressWarnings("unused")
	private void changePresence(String message) {
		changePresence(buildOnlinePresence(message));
	}

	private void changePresence(ClientPresence presence) {
		nhlBot.getDiscordManager().changePresence(presence);
	}

	private ClientPresence getOnlineStatus() {
		String status = Config.STATUS_MESSAGE;
		Team team = Config.DEFAULT_TEAM;
		Game nextGame = nhlBot.getGameScheduler().getNextGame(team);
		if(nextGame != null) {
			Team oppTeam = nextGame.getOppossingTeam(team);
			if (oppTeam != null) {				
				String nextGameMessage = String.format("next in #%s.", GameDayChannel.buildChannelName(nextGame));
				status = nextGameMessage + status;
			}
		}
		return buildOnlinePresence(status);
	}

	/**
	 * Gets the date in the format "d/MM"
	 * 
	 * @param game
	 *            game to get the date from
	 * @param zone
	 *            time zone to convert the time to
	 * @return the date in the format "d/MM"
	 */
	public static String buildPresenceDate(Game game, ZoneId zone) {
		return game.getStartTime().withZoneSameInstant(zone).format(DateTimeFormatter.ofPattern("d/MM"));
	}

	private static ClientPresence buildOnlinePresence(String message) {
		return ClientPresence.online(ClientActivity.streaming(message, Config.GIT_URL));
	}

	@Override
	public void run() {
		changePresenceToFetchSchedule();

		LocalDate lastUpdate = null;
		while (!isStop()) {
			LocalDate schedulerUpdate = nhlBot.getGameScheduler().getLastUpdate();
			if (schedulerUpdate == null) {
				LOGGER.info("Waiting for GameScheduler to initialize...");
				Utils.sleep(INIT_UPDATE_RATE);
			} else if (lastUpdate == null || schedulerUpdate.compareTo(lastUpdate) > 0) {
				LOGGER.info("Updating Status.");
				changePresence(getOnlineStatus());
				lastUpdate = schedulerUpdate;
			} else {
				Utils.sleep(UPDATE_RATE);
			}
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
