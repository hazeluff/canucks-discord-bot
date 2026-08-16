package com.hazeluff.discord.bot.command;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.TopLevelMessageComponent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Displays information about NHLBot and the author
 */
public class ConfigPlayoffCommand extends Command {
	static final String NAME = "config-playoff";

	public ConfigPlayoffCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	public String getName() {
		return NAME;
	}

	public ApplicationCommandRequest getACR() {
		return ApplicationCommandRequest.builder()
			.name(getName())
			.description("Configure the Playoff Watch Channels.")
            .build();
	}

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {
		Guild guild = getGuild(event);
		Member user = event.getInteraction().getMember().orElse(null);
		if (!hasPrivilege(guild, user)) {
			return event.reply(MUST_HAVE_PERMISSIONS_MESSAGE);
		}

		return event.reply()
			.withEphemeral(true)
			.withContent("Turns on/off the `#playoff-watch` channel.\n"
				+ "Select which configuration you want:\n"
				+ "**Turn Off** - Disables the features.\n"
				+ "**Single Channel** - All game messages will be sent to a single channel `#playoff-watch`.\n"
				+ "**Individual Threads** - Each game will be created as a separate threads within #playoff-watch.\n")
			.withComponents(buildNHLConfigComponent());
		
	}

	public final static String OFF_BUTTON_ID = "nhl-config-playoff-off";
	public final static String SINGLE_BUTTON_ID = "nhl-config-playoff-single";
	public final static String THREAD_BUTTON_ID = "nhl-config-playoff-thread";

	private static TopLevelMessageComponent buildNHLConfigComponent() {
		return ActionRow.of(
			Button.secondary(OFF_BUTTON_ID, "Turn Off"),
			Button.secondary(SINGLE_BUTTON_ID, "Single Channel"),
			Button.secondary(THREAD_BUTTON_ID, "Individual Threads")
		);
	}
}
