package com.hazeluff.discord.bot.command.config;

import java.util.function.Supplier;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.ConfigCommand;
import com.hazeluff.discord.bot.command.InteractionUtils;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.bot.gdc.nhl.NHLGameDayWatchChannel;
import com.hazeluff.discord.bot.listener.EventListener;

import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.InteractionReplyEditSpec;

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
			changeGameDayChannelType(guild, false);
			break;
		case ConfigCommand.THREAD_BUTTON_ID:
			changeGameDayChannelType(guild, true);
			break;
		}
	}

	private void changeGameDayChannelType(Guild guild, boolean useThread) {
		Long guildId = guild.getId().asLong();
		GuildPreferences pref = nhlBot.getPersistentData().getPreferencesData().getGuildPreferences(guildId);
		
		if(pref.isUseChannelThreads() == useThread)
			return;
		
		// Update preferences; save to DB
		pref.setUseChannelThreads(useThread);
		nhlBot.getPersistentData().getPreferencesData().savePreferences(guildId, pref);

		// Update the GameDayWatchChannel
		if (!pref.getTeams().isEmpty()) {
			NHLGameDayWatchChannel gdwChannel = NHLGameDayWatchChannel.getChannel(guildId);
			if (gdwChannel == null)
				gdwChannel = NHLGameDayWatchChannel.getOrCreate(nhlBot, guild);
			else
				gdwChannel.changeThreadUsage(useThread);
		}
	}
	
	InteractionReplyEditSpec buildReplyEdit(ButtonInteractionEvent event) {
		String message = "Done";
		switch (event.getCustomId()) {
		case ConfigCommand.SINGLE_BUTTON_ID:
			message = "Games now post directly to #game-day-watch";
			break;
		case ConfigCommand.THREAD_BUTTON_ID:
			message = "Games now post to threads within #game-day-watch";
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
