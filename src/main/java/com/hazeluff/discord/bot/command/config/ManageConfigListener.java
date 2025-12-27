package com.hazeluff.discord.bot.command.config;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.ConfigCommand;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.bot.gdc.nhl.NHLGameDayWatchChannel;
import com.hazeluff.discord.bot.listener.EventListener;

import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Guild;

public class ManageConfigListener extends EventListener {
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
		Guild guild = DiscordManager.block(event.getInteraction().getGuild());
		switch(event.getCustomId()) {
		case ConfigCommand.SINGLE_BUTTON_ID:
			changeGameDayChannelType(guild, true);
			break;
		case ConfigCommand.INDIVIDUAL_BUTTON_ID:
			changeGameDayChannelType(guild, false);
			break;
		}
	}

	private void changeGameDayChannelType(Guild guild, boolean singleChannel) {
		Long guildId = guild.getId().asLong();
		if (singleChannel) {
			// Preferences is set within #getOrCreateChannel
			// Start up NHLGameDayWatchChannel
			NHLGameDayWatchChannel.getOrCreateChannel(nhlBot, guild);
			// Remove from GameDayChannelsManager
			nhlBot.getGameDayChannelsManager().removeGuild(guildId);
		} else { // individual channels
			nhlBot.getPersistentData().getPreferencesData().setGameDayChannelId(guildId, 0l);
			// Remove NHLGameDayWatchChannel
			NHLGameDayWatchChannel.removeChannel(guildId);
			// Start up GameDayChannelsManager
			nhlBot.getGameDayChannelsManager().updateChannels(guild);
		}
	}
}
