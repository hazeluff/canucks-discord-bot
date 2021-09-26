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
import reactor.core.publisher.Mono;

/**
 * Displays information about NHLBot and the author
 */
public class AboutCommand extends Command {

	public AboutCommand(NHLBot nhlBot) {
		super(nhlBot);
	}
	
	public String getName() {
		return "about";
	}
	
	public ApplicationCommandRequest getACR() {
		return ApplicationCommandRequest.builder()
				.name(getName())
				.description("Learn a little about " + Config.APPLICATION_NAME + " and its creator.")
                .build();
	}

	@Override
	public Publisher<?> onChatInputInteraction(ChatInputInteractionEvent event) {
		if (event.getCommandName().equals(getName())) {
			return event.reply(getSpec());
		}
		return Mono.empty();
	}

	public Consumer<InteractionApplicationCommandCallbackSpec> getSpec() {
		Consumer<EmbedCreateSpec> embedCreateSpec = spec -> spec
				.setColor(Color.of(0xba9ddf))
				.setTitle("About " + Config.APPLICATION_NAME)
				.setAuthor("Hazeluff", Config.HAZELUFF_SITE, null)
				.setUrl(Config.GIT_URL)
				.setDescription(
						"A bot that provides information about NHL games, "
								+ "and creates channels that provides game information in real time.")
				.addField("Discord", Config.HAZELUFF_MENTION, true)
				.addField("Twitter", Config.HAZELUFF_TWITTER, true)
				.addField("Email", Config.HAZELUFF_EMAIL, true)
				.addField("Version", Config.VERSION, false)
				.addField("GitHub", Config.GIT_URL, true)
				.addField(
						"Studying Programming? Want to contribute?",
						"If you’re an aspiring programmer/student looking to get experience, "
								+ "I am more than willing to work with you to improve the bot. Just shoot me a message!",
						false)
				.addField(
						"Donations",
						"I support this bot personally. "
								+ "Donations will help offset my costs of running the server."
								+ "\n**Paypal**: " + Config.DONATION_URL
								+ "\n**DOGE**: " + Config.DONATION_DOGE
								+ "\n**ETH**: " + Config.DONATION_ETH,
						false);
		return spec -> spec.addEmbed(embedCreateSpec);
	}
}
