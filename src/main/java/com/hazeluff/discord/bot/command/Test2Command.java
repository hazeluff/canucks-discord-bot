package com.hazeluff.discord.bot.command;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Displays information about NHLBot and the author
 */
public class Test2Command extends Command {
	static final String NAME = "testqwer";

	public Test2Command(NHLBot nhlBot) {
		super(nhlBot);
	}

	public String getName() {
		return NAME;
	}

	public ApplicationCommandRequest getACR() {
		return ApplicationCommandRequest.builder()
				.name(getName())
				.description("foo bar 3")
                .build();
	}

	@Override
	public boolean isDevOnly() {
		return false;
	}

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {
		return event.reply(spec -> spec.setContent("test"));
	}
}
