package com.hazeluff.discord.bot;


import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.command.AboutCommand;
import com.hazeluff.discord.bot.command.HelpCommand;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.MessageCreateSpec;

public class WelcomeChannel extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(WelcomeChannel.class);

	private static final String CHANNEL_NAME = "welcome";

	// Update every hour
	private static final Consumer<MessageCreateSpec> UPDATED_MESSAGE = spec -> spec
			.setContent("I was just deployed/restarted.");

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
			channel = nhlBot.getDiscordManager().createAndGetChannel(guild, CHANNEL_NAME);
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
			channel.getMessagesBefore(lastMessageId).collectList().block().stream()
					.filter(message -> nhlBot.getDiscordManager().isAuthorOfMessage(message))
					.forEach(message -> nhlBot.getDiscordManager().deleteMessage(message));
			nhlBot.getDiscordManager().deleteMessage(
					nhlBot.getDiscordManager().block(channel.getLastMessage()));
		}
		nhlBot.getDiscordManager().sendMessage(channel, UPDATED_MESSAGE);
		nhlBot.getDiscordManager().sendMessage(channel, spec -> {
			spec.setContent("About the bot:");
			spec.addEmbed(AboutCommand.EMBED_SPEC);
		});
		nhlBot.getDiscordManager().sendMessage(channel, HelpCommand.COMMAND_LIST);
	}
}
