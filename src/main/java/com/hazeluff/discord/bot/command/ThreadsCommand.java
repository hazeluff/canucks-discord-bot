package com.hazeluff.discord.bot.command;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.utils.DiscordThreadFactory;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;

public class ThreadsCommand extends Command {

	static final String NAME = "threads";

	public ThreadsCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	public String getName() {
		return NAME;
	}

	public ApplicationCommandRequest getACR() {
		return null;
	}

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {
		return event.reply("Threads: " + DiscordThreadFactory.getInstance().getThreads().size());
	}
}
