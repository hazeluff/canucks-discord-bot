package com.hazeluff.discord.canucks.bot.command;

import java.util.List;
import java.util.function.Consumer;

import com.hazeluff.discord.canucks.bot.CanucksBot;
import com.hazeluff.discord.canucks.utils.DiscordThreadFactory;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;

public class ThreadsCommand extends Command {


	public ThreadsCommand(CanucksBot canucksBot) {
		super(canucksBot);
	}

	@Override
	public void execute(MessageCreateEvent event, List<String> arguments) {
		sendMessage(event, getReply());
	}

	@Override
	public boolean isAccept(Message message, List<String> arguments) {
		return arguments.get(0).equalsIgnoreCase("threads");
	}

	public Consumer<MessageCreateSpec> getReply() {
		return spec -> spec.setContent("Threads: " + DiscordThreadFactory.getInstance().getThreads().size());
	}
}