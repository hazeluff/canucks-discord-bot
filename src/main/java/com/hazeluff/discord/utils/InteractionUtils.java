package com.hazeluff.discord.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.core.spec.InteractionReplyEditSpec;
import reactor.core.publisher.Mono;

public class InteractionUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(InteractionUtils.class);

	public static String getOptionAsString(DeferrableInteractionEvent event, String option) {
		return event.getInteraction().getCommandInteraction().get().getOption(option)
				.flatMap(ApplicationCommandInteractionOption::getValue)
				.map(ApplicationCommandInteractionOptionValue::asString)
				.orElse(null);
	}

	public static Long getOptionAsLong(DeferrableInteractionEvent event, String option) {
		return event.getInteraction().getCommandInteraction().get().getOption(option)
				.flatMap(ApplicationCommandInteractionOption::getValue)
				.map(ApplicationCommandInteractionOptionValue::asLong).orElse(null);
	}

	public static Mono<Channel> getOptionAsChannel(DeferrableInteractionEvent event, String option) {
		return event.getInteraction().getCommandInteraction().get().getOption(option)
				.flatMap(ApplicationCommandInteractionOption::getValue)
				.map(ApplicationCommandInteractionOptionValue::asChannel)
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

	public static Mono<Message> replyAndDefer(
		DeferrableInteractionEvent event, 
		String initialReply,
		Runnable defferedAction, 
		Supplier<InteractionFollowupCreateSpec> defferedReplySupplier
	) {
		return event.reply(buildReplySpec(initialReply, null, true))
				.then(Mono.defer(() -> {
					defferedAction.run();
					return event.createFollowup(defferedReplySupplier.get());
		}));
	}
	
	public static Mono<Message> replyAndDeferEdit(
		DeferrableInteractionEvent event,
		String initialReply,
		Runnable defferedAction,
		Supplier<InteractionReplyEditSpec> defferedReplySupplier
	) {
		return event.reply(buildReplySpec(initialReply, null, true))
			.then(Mono.defer(() -> {
				try {
					defferedAction.run();
					return event.editReply(defferedReplySupplier.get());
				} catch (Exception e) {
					LOGGER.error("Error occured.", e);
					return event.editReply(buildReplyEditSpec("Error occured."));
				}
			})
		);
	}
	
	public static Mono<Message> replyAndDeferEdit(
		DeferrableInteractionEvent event,
		String initialReply,
		DefferableActionReplyWithMemory defferedAction
	) {
		return replyAndDeferEdit(event, initialReply, 
			defferedAction.getDefferedAction(),
			defferedAction.getDefferedReply());
	}
	
	public static InteractionFollowupCreateSpec buildFollowUpSpec(String message) {
		return InteractionFollowupCreateSpec.builder()
				.content(message)
				.build();
	}
	
	public static InteractionFollowupCreateSpec buildFollowUpSpec(String message, boolean ephemeral) {
		return InteractionFollowupCreateSpec.builder()
				.content(message)
				.ephemeral(ephemeral)
				.build();
	}
	
	public static InteractionReplyEditSpec buildReplyEditSpec(String message) {
		return InteractionReplyEditSpec.builder()
				.contentOrNull(message)
				.build();
	}

	public class DefferableActionReplyWithMemory {
		private final Consumer<Map<String, Object>> defferedAction;
		private final Function<Map<String, Object>, InteractionReplyEditSpec> defferedReply;
		private final Map<String, Object> memory;

		public DefferableActionReplyWithMemory(Consumer<Map<String, Object>> defferedAction,
			Function<Map<String, Object>, InteractionReplyEditSpec> defferedReply) {
			this.defferedAction = defferedAction;
			this.defferedReply = defferedReply;
			this.memory = new ConcurrentHashMap<>();
		}

		public Runnable getDefferedAction() {
			return () -> defferedAction.accept(memory);
		}

		public Supplier<InteractionReplyEditSpec> getDefferedReply() {
			return () -> defferedReply.apply(memory);
		}
	}
}
