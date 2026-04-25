package com.hazeluff.discord.bot.command;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.discord.DiscordManager;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Displays help for the NHLBot commands
 */
public class SayCommand extends Command {
	private static final Logger LOGGER = LoggerFactory.getLogger(SayCommand.class);

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
                .name("message")
                .description("The thing to say.")
				.type(ApplicationCommandOption.Type.STRING.getValue())
				.required(true)
                .build())
            .addOption(ApplicationCommandOptionData.builder()
                .name("channel")
                .description("Channel to say the message in.")
                .type(ApplicationCommandOption.Type.CHANNEL.getValue())
				.required(false)
                .build())
            .addOption(ApplicationCommandOptionData.builder()
				.name("ref-msg")
				.description("Message to reference.")
				.type(ApplicationCommandOption.Type.STRING.getValue())
				.required(false)
                .build())
			.build();
	}

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {
		String message = getOptionAsString(event, "message");
		Long channelId = getOptionAsLong(event, "channel");
		String refMessageId = getOptionAsString(event, "ref-msg");
		return replyAndDeferEdit(
			event,
			"Sending..", 
			() -> {
				// Log usage in #bot-admin
				MessageChannel logChannel = DiscordManager
					.getTextChannel(DiscordManager.block(event.getInteraction().getGuild()), "bot-admin");
				if (logChannel == null) {
					LOGGER.warn("Could not find #bot-admin channel.");
					return;
				}

				MessageChannel msgChannel = channelId == null
					? DiscordManager.block(event.getInteraction().getChannel().cast(MessageChannel.class))
					: DiscordManager.block(event.getOptionAsChannel("channel").cast(MessageChannel.class));

				DiscordManager.sendMessage(logChannel,
					buildLogMessage(event.getUser().getUsername(), msgChannel.getId().asLong(), message, refMessageId));

				MessageCreateSpec.Builder msgSpec = MessageCreateSpec.builder();
				msgSpec.content(message);
				if (refMessageId != null)
					msgSpec.messageReference(InteractionUtils.toMessageReferenceData(Long.valueOf(refMessageId)));
				DiscordManager.subscribe(msgChannel.createMessage(msgSpec.build()));
			}, 
			() -> buildReplyEditSpec("Sent.")
		);
	}

	private String buildLogMessage(String username, Long channelId, String message, String refId) {
		return String.format("`/say` used - **message** `%s`\nuser `%s` channel `%s` ref-msg `%s`",
			message, username, channelId, refId);
	}
}
