package com.hazeluff.discord.bot.command;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Displays information about NHLBot and the author
 */
public class TestCommand extends Command {
	static final String NAME = "test";

	public TestCommand(NHLBot nhlBot) {
		super(nhlBot);
	}
	
	public String getName() {
		return NAME;
	}
	
	public ApplicationCommandRequest getACR() {
		return ApplicationCommandRequest.builder()
				.name(getName())
				.description("foo bar")
                .build();
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {
		return event.reply(spec -> spec.setContent("test"));
	}
}
