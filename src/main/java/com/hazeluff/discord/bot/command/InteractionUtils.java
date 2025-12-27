package com.hazeluff.discord.bot.command;

import java.util.function.Supplier;

import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import reactor.core.publisher.Mono;

public class InteractionUtils {
	public static String getOptionAsString(DeferrableInteractionEvent event, String option) {
		return event.getInteraction().getCommandInteraction().get().getOption(option)
				.flatMap(ApplicationCommandInteractionOption::getValue)
				.map(ApplicationCommandInteractionOptionValue::asString)
				.orElse(null);
	}

	public static Long getOptionAsLong(DeferrableInteractionEvent event, String option) {
		return event.getInteraction().getCommandInteraction().get().getOption(option)
				.flatMap(ApplicationCommandInteractionOption::getValue)
				.map(ApplicationCommandInteractionOptionValue::asLong)
				.orElse(null);
	}

	public static Mono<Void> reply(DeferrableInteractionEvent event, String message) {
		return reply(event, message, null, false);
	}

	public static Mono<Void> reply(DeferrableInteractionEvent event, String message, boolean ephermeral) {
		return reply(event, message, null, ephermeral);
	}

	public static Mono<Void> reply(DeferrableInteractionEvent event, EmbedCreateSpec embedCreateSpec) {
		return reply(event, null, embedCreateSpec, false);
	}

	public static Mono<Void> reply(DeferrableInteractionEvent event, EmbedCreateSpec embedCreateSpec,
			boolean ephermeral) {
		return reply(event, null, embedCreateSpec, ephermeral);
	}

	public static Mono<Void> reply(DeferrableInteractionEvent event, String message, EmbedCreateSpec embedCreateSpec,
			boolean ephermeral) {
		InteractionApplicationCommandCallbackSpec spec = buildReplySpec(message, embedCreateSpec, ephermeral);
		return event.reply(spec);
	}

	static InteractionApplicationCommandCallbackSpec buildReplySpec(String message, EmbedCreateSpec embedCreateSpec,
			boolean ephermeral) {
		InteractionApplicationCommandCallbackSpec.Builder builder = InteractionApplicationCommandCallbackSpec.builder();
		if (message != null) {
			builder.content(message);
		}
		builder.ephemeral(ephermeral);
		if (embedCreateSpec != null) {
			builder.addEmbed(embedCreateSpec);
		}
		return builder.build();
	}

	public static Mono<Message> replyAndDefer(DeferrableInteractionEvent event, String initialReply,
			Supplier<InteractionFollowupCreateSpec> defferedReplySupplier) {
		return replyAndDefer(event, initialReply, () -> {
		}, defferedReplySupplier);
	}

	public static Mono<Message> replyAndDefer(DeferrableInteractionEvent event, String initialReply,
			Runnable defferedAction, Supplier<InteractionFollowupCreateSpec> defferedReplySupplier) {
		return event.reply(buildReplySpec(initialReply, null, true)).then(Mono.defer(() -> {
			defferedAction.run();
			return createSlowFollowUp(event, defferedReplySupplier);
		}));
	}

	/**
	 * 
	 * @param event
	 * @param specSupplier
	 *            Specification that takes time to create.
	 * @return
	 */
	public static Mono<Message> createSlowFollowUp(DeferrableInteractionEvent event,
			Supplier<InteractionFollowupCreateSpec> specSupplier) {
		return event.createFollowup(specSupplier.get());
	}
}
