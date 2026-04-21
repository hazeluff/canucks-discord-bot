package com.hazeluff.discord.bot;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.utils.InterruptableThread;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.game.NHLGame;

import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;

public class PresenceManager extends InterruptableThread {
	private static final Logger LOGGER = LoggerFactory.getLogger(PresenceManager.class);

	private static final ClientPresence STARTING_UP_PRESENCE = ClientPresence
			.doNotDisturb(ClientActivity.watching("itself starting up..."));

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

	@SuppressWarnings("unused")
	private void changePresence(String message) {
		changePresence(buildOnlinePresence(message));
	}

	private void changePresence(ClientPresence presence) {
		nhlBot.getDiscordManager().changePresence(presence);
	}

	private ClientPresence getOnlineStatus() {
		return buildOnlinePresence(Utils.getRandom(Config.STATUS_MESSAGES));
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
	public static String buildPresenceDate(NHLGame game, ZoneId zone) {
		return game.getStartTime().withZoneSameInstant(zone).format(DateTimeFormatter.ofPattern("d/MM"));
	}

	private static ClientPresence buildOnlinePresence(String message) {
		if (message.startsWith("Watching: ")) {
			String watchingMsg = message.replace("Watching: ", "");
			return ClientPresence.online(ClientActivity.watching(watchingMsg));
		}
		if (message.startsWith("Listening: ")) {
			String listeningMsg = message.replace("Listening: ", "");
			return ClientPresence.online(ClientActivity.listening(listeningMsg));
		}
		if (message.startsWith("Playing: ")) {
			String playingMsg = message.replace("Playing: ", "");
			return ClientPresence.online(ClientActivity.playing(playingMsg));
		}
		
		return ClientPresence.online(ClientActivity.streaming(message, Config.GIT_URL));
	}

	@Override
	public void run() {
		int lastUpdateHour = -1;
		while (!isStop() && !isInterrupted()) {
			try {
				LocalDateTime now = LocalDateTime.now();
				if (lastUpdateHour == -1 || now.getHour() != lastUpdateHour) {
					LOGGER.info("Updating Status.");
					changePresence(getOnlineStatus());
					lastUpdateHour = now.getHour();
				} else {
					sleepFor(UPDATE_RATE);
				}
			} catch (Throwable t) {
				LOGGER.error("Error occurred when updating Presence.", t);
				sleepFor(UPDATE_RATE);
			}
		}
	}
}
