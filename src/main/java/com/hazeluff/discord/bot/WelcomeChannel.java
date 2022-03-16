package com.hazeluff.discord.bot;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.command.AboutCommand;
import com.hazeluff.discord.bot.command.HelpCommand;
import com.hazeluff.discord.bot.discord.DiscordManager;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.MessageCreateSpec;

public class WelcomeChannel extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(WelcomeChannel.class);

	private static final String CHANNEL_NAME = "welcome";

	private static final String UPDATED_MESSAGE = "I was just deployed/restarted.";

	private final NHLBot nhlBot;
	private final TextChannel channel;

	WelcomeChannel(NHLBot nhlBot, TextChannel channel) {
		this.nhlBot = nhlBot;
		this.channel = channel;
	}

	public static WelcomeChannel createChannel(NHLBot nhlBot, Guild guild) {
		TextChannel channel;
		try {
			channel = guild.getChannels()
					.filter(TextChannel.class::isInstance)
					.cast(TextChannel.class)
					.filter(guildChannel -> guildChannel.getName().equals(CHANNEL_NAME))
					.take(1)
					.onErrorReturn(null)
					.blockFirst();
		} catch (Exception e) {
			channel = DiscordManager.createAndGetChannel(guild, CHANNEL_NAME);
		}
		WelcomeChannel welcomeChannel = new WelcomeChannel(nhlBot, channel);
		welcomeChannel.start();
		return welcomeChannel;
	}

	@Override
	public void run() {
		if (channel == null) {
			LOGGER.warn("Channel could not found in Discord.");
			return;
		}

		Snowflake lastMessageId = channel.getLastMessageId().orElse(null);
		if (lastMessageId != null) {
			DiscordManager.block(channel.getMessagesBefore(lastMessageId)).stream()
					.filter(message -> nhlBot.getDiscordManager().isAuthorOfMessage(message))
					.forEach(message -> DiscordManager.deleteMessage(message));
			DiscordManager.deleteMessage(DiscordManager.block(channel.getLastMessage()));
		}
		DiscordManager.sendMessage(channel, UPDATED_MESSAGE);
		DiscordManager.sendMessage(channel, MessageCreateSpec
				.builder()
			.content("About the bot:")
			.addEmbed(AboutCommand.EMBED_SPEC)
			.build()
		);
		DiscordManager.sendMessage(channel, HelpCommand.COMMAND_LIST);
	}
}
