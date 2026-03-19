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
public class ConfigCommand extends Command {
	static final String NAME = "config";

	public ConfigCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	public String getName() {
		return NAME;
	}

	public ApplicationCommandRequest getACR() {
		return ApplicationCommandRequest.builder()
				.name(getName())
				.description("Configure the bot. Use this on your own server.")
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
				.withContent("Select which configuration you want:\n"
						+ "**Single Channel** - All games will be created in a single channel `#game-day-watch` (default)\n"
						+ "**Individual Thread** - Each game will be created as a separate threads within #game-day-watch.\n"
						+ "\tThe most recent, and next upcoming games are maintained as channels (max. 2 at a time).\n"
						+ "\tOlder games/channels are deleted.")
				.withComponents(buildNHLConfigComponent());
		
	}

	public final static String SINGLE_BUTTON_ID = "nhl-config-single";
	public final static String THREAD_BUTTON_ID = "nhl-config-thread";

	private static TopLevelMessageComponent buildNHLConfigComponent() {
		return ActionRow.of(
			Button.secondary(SINGLE_BUTTON_ID, "Single Channel"),
			Button.secondary(THREAD_BUTTON_ID, "Individual Threads")
		);
	}
}
