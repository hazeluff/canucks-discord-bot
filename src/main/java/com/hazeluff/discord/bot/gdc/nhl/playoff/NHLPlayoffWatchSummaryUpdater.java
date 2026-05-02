package com.hazeluff.discord.bot.gdc.nhl.playoff;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.channel.playoff.PlayoffWatchMeta;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.utils.DateUtils;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.NHLGateway;
import com.hazeluff.nhl.PlayoffSeries;
import com.hazeluff.nhl.game.NHLGame;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;

public class NHLPlayoffWatchSummaryUpdater extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(NHLPlayoffWatchSummaryUpdater.class);

	static final long UPDATE_RATE = 300000L;

	private final NHLBot nhlBot;
	private final TextChannel channel;
	private final PlayoffWatchMeta meta;

	private EmbedCreateSpec summaryMessageEmbed;
	private Message summaryMessage;
	private EmbedCreateSpec scheduleMessageEmbed;
	private Message scheduleMessage;

	NHLPlayoffWatchSummaryUpdater(NHLBot nhlBot, TextChannel channel, PlayoffWatchMeta meta) {
		this.nhlBot = nhlBot;
		this.channel = channel;
		this.meta = meta;
	}

	@Override
	public void run() {
		LocalDateTime lastUpdate = null;
		initSummaryMessage();
		initScheduleMessage();
		while (!isStop()) {
			try {
				LocalDateTime currentTime = LocalDateTime.now();
				// Update every hour (hour # is different)
				if (lastUpdate == null || lastUpdate.getHour() != currentTime.getHour()) {
					LOGGER.info("Updating Channels...");
					try {
						updateSummaryMessage();
						updateScheduleMessage();
					} catch (Exception e) {
						LOGGER.warn("Failed to update messages.", e);
					}
					lastUpdate = currentTime;
				} else {
					LOGGER.debug("Waiting for next update.");
					Utils.sleep(UPDATE_RATE);
				}
			} catch (Exception e) {
				LOGGER.error("Error occured when updating channels.", e);
			}
		}
	}

	private void updateScheduleMessage() {
		LOGGER.info("Updating Playoff Schedule Message.");
		if (scheduleMessage != null) {
			EmbedCreateSpec newScheduleMessageEmbed = getScheduleEmbedSpec();
			if (!newScheduleMessageEmbed.equals(scheduleMessageEmbed)) {
				MessageEditSpec messageSpec = MessageEditSpec.builder().addEmbed(newScheduleMessageEmbed).build();
				DiscordManager.updateMessage(scheduleMessage, messageSpec);
			}
		} else {
			scheduleMessage = getSummaryMessage();
		}
	}

	private void updateSummaryMessage() {
		LOGGER.info("Updating Playoff Summary Message.");
		if (summaryMessage != null) {
			EmbedCreateSpec newSummaryMessageEmbed = getSummaryEmbedSpec();
			if (!newSummaryMessageEmbed.equals(summaryMessageEmbed)) {
				MessageEditSpec messageSpec = MessageEditSpec.builder().addEmbed(newSummaryMessageEmbed).build();
				DiscordManager.updateMessage(summaryMessage, messageSpec);
			}
		} else {
			summaryMessage = getSummaryMessage();
		}
	}

	/*
	 * Schedule Message
	 */
	public void initScheduleMessage() {
		if (scheduleMessage == null) {
			LOGGER.info("Init Schedule Message.");
			scheduleMessage = getScheduleMessage();
		}
	}

	private Message getScheduleMessage() {
		Message message = null;
		if (meta != null) {
			Long messageId = meta.getScheduleMessageId();
			if (messageId == null) {
				// No message saved
				message = sendScheduleMessage();
			} else {
				message = nhlBot.getDiscordManager().getMessage(channel.getId().asLong(), messageId);
				if (message == null) {
					// Could not find existing message. Send new message
					message = sendScheduleMessage();
				} else {
					// Message exists
					return message;
				}
			}

			if (message != null) {
				DiscordManager.pinMessage(message);
				meta.setScheduleMessageId(message.getId().asLong());
				saveMetadata();
			}
		}
		return message;
	}

	private Message sendScheduleMessage() {
		LOGGER.info("Sending Schedule Message.");
		scheduleMessageEmbed = getScheduleEmbedSpec();
		MessageCreateSpec messageSpec = MessageCreateSpec.builder().addEmbed(scheduleMessageEmbed).build();
		return DiscordManager.sendAndGetMessage(channel, messageSpec);
	}

	protected EmbedCreateSpec getScheduleEmbedSpec() {
		Map<String, PlayoffSeries> playoffBracket = NHLGateway
			.getPlayoffBracket(String.valueOf(Config.NHL_CURRENT_SEASON.getEndYear()));
		
		EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();

		List<PlayoffSeries> activeSeries = new ArrayList<PlayoffSeries>();
		for (PlayoffSeries series : playoffBracket.values())
		{
			if (series.isParticipantsSet() && !series.hasWinningTeam())
				activeSeries.add(series);
		}
		
		embedBuilder.title("Upcoming Games");
		// Should use roundNum, but NHL API is WRONG
		// for (int roundNum = 4; roundNum > 0; roundNum--) { }
		for (String abbrev : Arrays.asList("SCF", "CF", "R2", "R1")) {
			StringBuilder strB = new StringBuilder();
			for(PlayoffSeries series : activeSeries)
			{
				if (strB.length() > 0)
					strB.append("\n");
				if (series.getSeriesAbbrev().equals(abbrev))
					strB.append(buildNextGameLine(series));
			}

			String roundName = "Round?";
			switch (abbrev)
			{
			case "SCF":
				roundName = "Stanley Cup Finals";
				break;
			case "CF":
				roundName = "Conference Finals";
				break;
			case "R2":
				roundName = "Round 2";
				break;
			case "R1":
				roundName = "Round 1";
				break;
			}

			if (strB.length() > 0)
				embedBuilder.addField(roundName, strB.toString(), false);
		}

		return embedBuilder.build();
	}

	private String buildNextGameLine(PlayoffSeries series) {
		NHLGame nextGame = nhlBot.getNHLGameScheduler().getNextGame(series.getTopSeedTeam());
		String matchup;
		if(nextGame != null)
		{
			matchup = String.format("%s vs %s - %s", 
				nextGame.getHomeTeam().getName(), nextGame.getAwayTeam().getName(), 
				DateUtils.toDiscordTS(nextGame.getStartTime()));
		
		}
		else
		{
			String topSeed = series.isTopSeedDetermined() ? series.getTopSeedTeam().getName() : "TBD";
			if (series.isTopSeedDetermined() && !series.isBottomSeedDetermined())
				topSeed = "**" + topSeed + "**";
			String btmSeed = series.isBottomSeedDetermined() ? series.getBottomSeedTeam().getName() : "TBD";
			if (series.isBottomSeedDetermined() && !series.isTopSeedDetermined())
				btmSeed = "**" + btmSeed + "**";
			matchup = String.format("%s vs %s", topSeed, btmSeed);
		}
		return matchup;
	}

	/*
	 * Summary Message
	 */
	public void initSummaryMessage() {
		if (summaryMessage == null) {
			LOGGER.info("Init Summary Message.");
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
		LOGGER.info("Sending Summary Message.");
		summaryMessageEmbed = getSummaryEmbedSpec();
		MessageCreateSpec messageSpec = MessageCreateSpec.builder().addEmbed(summaryMessageEmbed).build();
		return DiscordManager.sendAndGetMessage(channel, messageSpec);
	}

	protected EmbedCreateSpec getSummaryEmbedSpec() {
		Map<String, PlayoffSeries> playoffBracket = NHLGateway
				.getPlayoffBracket(String.valueOf(Config.NHL_CURRENT_SEASON.getEndYear()));
		EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();
		
		embedBuilder.title("Stanley Cup Playoffs - " + Config.NHL_CURRENT_SEASON.getEndYear());
		
		/*
		 * Round 1
		 */
		StringBuilder r1Str = new StringBuilder();
		for (PlayoffSeries series : playoffBracket.values()) {
			if (series.getSeriesAbbrev().equals("R1") && series.hasParticipant())
				appendSeriesToStr(r1Str, series);
		}
		embedBuilder.addField("Round 1", r1Str.toString(), false);
		
		/*
		 * Round 2
		 */
		StringBuilder r2Str = new StringBuilder();
		for (PlayoffSeries series : playoffBracket.values()) {
			if (series.getSeriesAbbrev().equals("R2") && series.hasParticipant())
				appendSeriesToStr(r2Str, series);
		}
		if (r2Str.length() == 0) {
			r2Str = new StringBuilder("Currently, no team has clinched.");
		}
		embedBuilder.addField("Round 2", r2Str.toString(), false);

		/*
		 * Conference Finals
		 */
		StringBuilder cfStr = new StringBuilder();
		for (PlayoffSeries series : playoffBracket.values()) {
			if (series.getSeriesAbbrev().equals("CF") && series.hasParticipant())
				appendSeriesToStr(cfStr, series);
		}
		if (cfStr.length() == 0) {
			cfStr = new StringBuilder("Currently, no team has clinched.");
		}
		embedBuilder.addField("Conference Finals", cfStr.toString(), false);

		/*
		 * Conference Finals
		 */
		PlayoffSeries scfSeries = playoffBracket.get("O");
		StringBuilder scfStr = new StringBuilder();
		if (scfSeries.hasParticipant()) {
			appendSeriesToStr(scfStr, scfSeries);
		} else {
			scfStr = new StringBuilder("Currently, no team has clinched.");
		}
		embedBuilder.addField("Stanley Cup Finals", scfStr.toString(), false);
		return embedBuilder.build();
	}
	
	private static StringBuilder appendSeriesToStr(StringBuilder strBuilder, PlayoffSeries series) {
		if (strBuilder.length() > 0) {
			strBuilder.append("\n");
		}
		
		switch(series.getSeriesAbbrev()) {
			case "CF":
				switch(series.getSeriesLetter()) {
					case "M":
						strBuilder.append("East: ");
						break;
					case "N":
						strBuilder.append("West: ");
						break;
					}
				break;
		}

		String topTeam = series.getTopSeedTeam() == null ? "TBD" : series.getTopSeedTeam().getName();
		String topTeamWins = String.valueOf(series.getTopSeedWins());
		if (series.getTopSeedWins() >= 4) {
			topTeam = "**" + topTeam + "**";
			topTeamWins = "**(" + topTeamWins + ")**";
		}
		else if (series.getTopSeedTeam() != null && series.getBottomSeedTeam() == null) {
			topTeam = "**" + topTeam + "**";
		}

		String btmTeam = series.getBottomSeedTeam() == null ? "TBD" : series.getBottomSeedTeam().getName();
		String btmTeamWins = String.valueOf(series.getBottomSeedWins());
		if (series.getBottomSeedWins() >= 4) {
			btmTeam = "**" + btmTeam + "**";
			btmTeamWins = "**(" + btmTeamWins + ")**";
		}
		else if (series.getBottomSeedTeam() != null && series.getTopSeedTeam() == null) {
			topTeam = "**" + btmTeam + "**";
		}

		strBuilder.append(String.format(
				"%s %s - %s %s",
				topTeam, topTeamWins, 
				btmTeamWins, btmTeam
		));
		return strBuilder;
	}

	void saveMetadata() {
		nhlBot.getPersistentData().getPlayoffWatchMetaData().save(meta);
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
