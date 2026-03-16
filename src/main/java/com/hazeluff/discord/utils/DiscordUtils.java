package com.hazeluff.discord.utils;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.channel.Channel;
import reactor.core.publisher.Mono;

public class DiscordUtils {
	public static String getOptionAsString(ChatInputInteractionEvent event, String option) {
		return event.getInteraction().getCommandInteraction().get().getOption(option)
				.flatMap(ApplicationCommandInteractionOption::getValue)
				.map(ApplicationCommandInteractionOptionValue::asString)
				.orElse(null);
	}

	public static Long getOptionAsLong(ChatInputInteractionEvent event, String option) {
		return event.getInteraction().getCommandInteraction().get().getOption(option)
				.flatMap(ApplicationCommandInteractionOption::getValue)
				.map(ApplicationCommandInteractionOptionValue::asLong)
				.orElse(null);
	}

	public static Mono<Channel> getOptionAsChannel(ChatInputInteractionEvent event, String option) {
		return event.getInteraction().getCommandInteraction().get().getOption(option)
				.flatMap(ApplicationCommandInteractionOption::getValue)
				.map(ApplicationCommandInteractionOptionValue::asChannel)
				.orElse(null);
	}
}
