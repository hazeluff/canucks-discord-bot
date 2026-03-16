package com.hazeluff.discord.bot.command;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.discord.DiscordManager;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Displays help for the NHLBot commands
 */
public class SayCommand extends Command {
	public static final String NAME = "say";

	public SayCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	public String getName() {
		return NAME;
	}

	public ApplicationCommandRequest getACR() {
		return ApplicationCommandRequest.builder()
				.name(getName())
				.description("Make me say something in another channel.")
				.defaultPermission(false)
                .addOption(ApplicationCommandOptionData.builder()
                        .name("channel")
                        .description("Channel to say the message in.")
                        .type(ApplicationCommandOption.Type.CHANNEL.getValue())
						.required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("message")
                        .description("The thing to say.")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.required(true)
                        .build())
				.build();
	}

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {
		String message = getOptionAsString(event, "message");
		return replyAndDeferEdit(
				event,
				"Sending..", 
				() -> {
					MessageChannel channel = DiscordManager
							.block(event.getOptionAsChannel("channel").cast(MessageChannel.class));
					DiscordManager.subscribe(channel.createMessage(message));
				}, 
				() -> buildReplyEditSpec("Sent.")
		);
	}


}
