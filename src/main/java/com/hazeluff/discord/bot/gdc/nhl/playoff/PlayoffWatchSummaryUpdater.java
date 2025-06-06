package com.hazeluff.discord.bot.gdc.nhl.playoff;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.channel.playoff.PlayoffWatchMeta;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.NHLGateway;
import com.hazeluff.nhl.PlayoffSeries;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;

public class PlayoffWatchSummaryUpdater extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlayoffWatchSummaryUpdater.class);

	static final long UPDATE_RATE = 300000L;

	private final NHLBot nhlBot;
	private final TextChannel channel;
	private final PlayoffWatchMeta meta;

	private EmbedCreateSpec summaryMessageEmbed;
	private Message summaryMessage;

	PlayoffWatchSummaryUpdater(NHLBot nhlBot, TextChannel channel, PlayoffWatchMeta meta) {
		this.nhlBot = nhlBot;
		this.channel = channel;
		this.meta = meta;
	}

	@Override
	public void run() {
		LocalDateTime lastUpdate = null;
		initSummaryMessage();
		while (!isStop()) {
			try {
				LocalDateTime currentTime = LocalDateTime.now();
				// Update every hour (hour # is different)
				if (lastUpdate == null || lastUpdate.getHour() != currentTime.getHour()) {
					LOGGER.info("Updating Channels...");
					try {
						updateSummaryMessage();
					} catch (Exception e) {
						LOGGER.warn("Failed to update message.", e);
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

	private void updateSummaryMessage() {
		if (summaryMessage != null) {
			EmbedCreateSpec newSummaryMessageEmbed = getSummaryEmbedSpec();
			if (!newSummaryMessageEmbed.equals(summaryMessageEmbed)) {
				MessageEditSpec messageSpec = MessageEditSpec.builder().addEmbed(newSummaryMessageEmbed).build();
				DiscordManager.updateMessage(summaryMessage, messageSpec);
			}
		}
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
		List<PlayoffSeries> r1Series = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H").stream()
				.map(seriesLetter -> playoffBracket.get(seriesLetter))
				.collect(Collectors.toList());
		StringBuilder r1Str = new StringBuilder();
		for (PlayoffSeries series : r1Series) {
			appendSeriesToStr(r1Str, series);
		}
		embedBuilder.addField("1st Round", r1Str.toString(), false);
		
		/*
		 * Round 2
		 */
		List<PlayoffSeries> r2Series = Arrays.asList("I", "J", "K", "L").stream()
				.map(seriesLetter -> playoffBracket.get(seriesLetter))
				.collect(Collectors.toList());
		StringBuilder r2Str = new StringBuilder();
		if (r2Series.stream().anyMatch(series -> series.hasParticipant())) {
			for (PlayoffSeries series : r2Series) {
				appendSeriesToStr(r2Str, series);
			}
		} else {
			r2Str = new StringBuilder("Currently, no team has clenched.");
		}
		embedBuilder.addField("2nd Round", r2Str.toString(), false);

		/*
		 * Conference Finals
		 */
		List<PlayoffSeries> cfSeries = Arrays.asList("M", "N").stream()
				.map(seriesLetter -> playoffBracket.get(seriesLetter))
				.collect(Collectors.toList());
		StringBuilder cfStr = new StringBuilder();
		if (cfSeries.stream().anyMatch(series -> series.hasParticipant())) {
			for (PlayoffSeries series : cfSeries) {
				appendSeriesToStr(cfStr, series);
			}
		} else {
			cfStr = new StringBuilder("Currently, no team has clenched.");
		}
		embedBuilder.addField("Conference Final", cfStr.toString(), false);

		/*
		 * Conference Finals
		 */
		PlayoffSeries scfSeries = playoffBracket.get("O");
		StringBuilder scfStr = new StringBuilder();
		if (scfSeries.hasParticipant()) {
			appendSeriesToStr(scfStr, scfSeries);
		} else {
			scfStr = new StringBuilder("Currently, no team has clenched.");
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

		String topTeam = series.getTopSeedTeam() == null ? "Not Decided" : series.getTopSeedTeam().getName();
		String topTeamWins = String.valueOf(series.getTopSeedWins());
		if (series.getTopSeedWins() >= 4) {
			topTeam = "**" + topTeam + "**";
			topTeamWins = "**(" + topTeamWins + ")**";
		}

		String btmTeam = series.getBottomSeedTeam() == null ? "Not Decided" : series.getBottomSeedTeam().getName();
		String btmTeamWins = String.valueOf(series.getBottomSeedWins());
		if (series.getBottomSeedWins() >= 4) {
			btmTeam = "**" + btmTeam + "**";
			btmTeamWins = "**(" + btmTeamWins + ")**";
		}

		strBuilder.append(String.format(
				"%s %s - %s %s",
				topTeam, topTeamWins, 
				btmTeamWins, btmTeam
		));
		return strBuilder;
	}

	private void saveMetadata() {
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
