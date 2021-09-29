package com.hazeluff.discord.bot.command;

import java.util.function.Consumer;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;

/**
 * Displays information about NHLBot and the author
 */
public class AboutCommand extends Command {
	static final String NAME = "about";

	public AboutCommand(NHLBot nhlBot) {
		super(nhlBot);
	}
	
	public String getName() {
		return NAME;
	}
	
	public ApplicationCommandRequest getACR() {
		return ApplicationCommandRequest.builder()
				.name(getName())
				.description("Learn a little about " + Config.APPLICATION_NAME + " and its creator.")
                .build();
	}

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {
		return event.reply(REPLY_SPEC);
	}

	public static final Consumer<EmbedCreateSpec> EMBED_SPEC = spec -> spec
			.setColor(Color.of(0xba9ddf))
			.setTitle("About " + Config.APPLICATION_NAME)
			.setAuthor("Hazeluff", Config.HAZELUFF_SITE, null).setUrl(Config.GIT_URL)
			.setDescription("A bot that provides information about NHL games, "
					+ "and creates channels that provides game information in real time.")
			.addField("Discord", Config.HAZELUFF_MENTION, true)
			.addField("Twitter", Config.HAZELUFF_TWITTER, true)
			.addField("Email", Config.HAZELUFF_EMAIL, true)
			.addField("Version", Config.VERSION, false)
			.addField("GitHub", Config.GIT_URL, true)
			.addField("Want to contribute?", "Just shoot me a message!", false)
			.addField(
					"Donations",
					"I support this bot personally. " + "Donations will help offset my costs of running the server."
							+ "\n**Paypal**: " + Config.DONATION_URL + "\n**DOGE**: " + Config.DONATION_DOGE
							+ "\n**ETH**: " + Config.DONATION_ETH,
					false);

	private static final Consumer<InteractionApplicationCommandCallbackSpec> REPLY_SPEC = spec -> spec
			.addEmbed(EMBED_SPEC);
}
