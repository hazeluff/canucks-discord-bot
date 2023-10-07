package com.hazeluff.discord.utils;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;

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
}
