package com.hazeluff.discord.bot.command.config;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.ConfigCommand;
import com.hazeluff.discord.bot.command.ConfigPlayoffCommand;
import com.hazeluff.discord.bot.database.preferences.GDCMode;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.database.preferences.PlayoffMode;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.bot.gdc.nhl.NHLGameDayWatchChannel;
import com.hazeluff.discord.bot.gdc.nhl.NHLGdcGuildManager;
import com.hazeluff.discord.bot.gdc.nhl.playoff.NHLPlayoffWatchChannel;
import com.hazeluff.discord.bot.listener.EventListener;
import com.hazeluff.discord.utils.InteractionUtils;

import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.InteractionReplyEditSpec;

public class ManageConfigListener extends EventListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageConfigListener.class);
	public ManageConfigListener(NHLBot nhlBot) {
		super(nhlBot);
	}

	@Override
	public void processEvent(Event event) {
		if (event instanceof ButtonInteractionEvent) {
			processEvent((ButtonInteractionEvent) event);
		}
	}

	public void processEvent(ButtonInteractionEvent event) {
		replyAndDeferEdit(event,
			"Processing your config change...",
			() -> processCommand(event),
			() -> {
				return buildReplyEdit(event);
			}
		);
	}

	private void processCommand(ButtonInteractionEvent event) {
		Guild guild = DiscordManager.block(event.getInteraction().getGuild());
		switch(event.getCustomId()) {
		case ConfigCommand.SINGLE_BUTTON_ID:
			changeGameDayChannelType(guild, GDCMode.SING_CHNL_NO_THRD);
			break;
		case ConfigCommand.THREAD_BUTTON_ID:
			changeGameDayChannelType(guild, GDCMode.SING_CHNL_W_THRD);
			break;
		case ConfigCommand.CHANNELS_BUTTON_ID:
			changeGameDayChannelType(guild, GDCMode.INDV_CHNL);
			break;
		case ConfigPlayoffCommand.OFF_BUTTON_ID:
			changePlayoffChannelType(guild, PlayoffMode.OFF);
			break;
		case ConfigPlayoffCommand.SINGLE_BUTTON_ID:
			changePlayoffChannelType(guild, PlayoffMode.SING_CHNL_NO_THRD);
			break;
		case ConfigPlayoffCommand.THREAD_BUTTON_ID:
			changePlayoffChannelType(guild, PlayoffMode.SING_CHNL_W_THRD);
			break;
		}
	}

	private void changeGameDayChannelType(Guild guild, GDCMode gdcMode) {
		Long guildId = guild.getId().asLong();
		GuildPreferences pref = nhlBot.getPersistentData().getPreferencesData().getGuildPreferences(guildId);
		GDCMode currentGDCMode = pref.getGDCMode();

		// No difference in preferences.
		if (currentGDCMode == gdcMode)
			return;

		// Update mode in preferences
		pref.setGDCMode(gdcMode);

		if (gdcMode.isIndvidualChannels()) {
			// Turn on NHLGdcGuildManager
			NHLGdcGuildManager.getAndStart(nhlBot, guild);

			// Turn off Game Day Watch channel.
			NHLGameDayWatchChannel.removeChannel(guildId);
			pref.setGameDayChannelId(null);
		} else {
			// Update the GameDayWatchChannel
			if (!pref.getTeams().isEmpty()) {
				NHLGameDayWatchChannel gdwChannel = NHLGameDayWatchChannel.getChannel(guildId);
				if (gdwChannel == null)
					// Turn on Game Day Watch channel.
					gdwChannel = NHLGameDayWatchChannel.getOrCreate(nhlBot, guild); // Uses new saved preferences.
				else
					gdwChannel.changeThreadUsage(gdcMode.isUseThreads());
			}

			// Turn off NHLGdcGuildManager.
			NHLGdcGuildManager.removeManager(guildId);
		}

		// Save Preferences to DB
		nhlBot.getPersistentData().getPreferencesData().savePreferences(guildId, pref);
	}

	private void changePlayoffChannelType(Guild guild, PlayoffMode playoffMode) {
		Long guildId = guild.getId().asLong();
		GuildPreferences pref = nhlBot.getPersistentData().getPreferencesData().getGuildPreferences(guildId);
		PlayoffMode currentMode = pref.getPlayoffMode();

		// No difference in preferences.
		if (currentMode == playoffMode)
			return;

		// Update mode in preferences
		pref.setPlayoffMode(playoffMode);

		if (!playoffMode.isEnabled()) {
			// Turn off channel
			NHLPlayoffWatchChannel.removeChannel(guildId);
			pref.setPlayoffChannelId(null);
		} 
		else {
			// Update channel
			NHLPlayoffWatchChannel channel = NHLPlayoffWatchChannel.get(guildId);
			if (channel == null)
				// Turn on channel
				channel = NHLPlayoffWatchChannel.getOrCreate(nhlBot, guild); // Uses new saved preferences.
			else
				channel.changeThreadUsage(playoffMode.isUseThreads());
		}

		// Save Preferences to DB
		nhlBot.getPersistentData().getPreferencesData().savePreferences(guildId, pref);
	}
	
	InteractionReplyEditSpec buildReplyEdit(ButtonInteractionEvent event) {
		String message = "Done";
		switch (event.getCustomId()) {
		case ConfigCommand.CHANNELS_BUTTON_ID:
			message = "Games now post to their own individual channels.";
			break;
		case ConfigCommand.SINGLE_BUTTON_ID:
			message = "Games now post directly to #game-day-watch";
			break;
		case ConfigCommand.THREAD_BUTTON_ID:
			message = "Games now post to threads within #game-day-watch";
			break;
		case ConfigPlayoffCommand.OFF_BUTTON_ID:
			message = "Playoffs Watch (#playoffs-watch) is turned off.";
			break;
		case ConfigPlayoffCommand.SINGLE_BUTTON_ID:
			message = "Playoff games now post directly to #playoffs-watch";
			break;
		case ConfigPlayoffCommand.THREAD_BUTTON_ID:
			message = "Playoff games now post to threads within #playoffs-watch";
			break;
		}

		return InteractionReplyEditSpec.builder().contentOrNull(message).build();
	}

	private void replyAndDeferEdit(
		ButtonInteractionEvent event,
		String initialReply,
		Runnable defferedAction,
		Supplier<InteractionReplyEditSpec> defferedReplySupplier) {
		DiscordManager
			.subscribe(InteractionUtils.replyAndDeferEdit(event, initialReply, defferedAction, defferedReplySupplier));
	}
}
