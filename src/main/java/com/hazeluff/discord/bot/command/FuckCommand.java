package com.hazeluff.discord.bot.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.utils.Utils;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

/**
 * Because fuck Mark Messier
 */
public class FuckCommand extends Command {
	private static final Logger LOGGER = LoggerFactory.getLogger(FuckCommand.class);

	static final String NAME = "fuck";

	public FuckCommand(NHLBot nhlBot) {
		super(nhlBot);
	}
	
	public String getName() {
		return NAME;
	}
	
	public ApplicationCommandRequest getACR() {
		return ApplicationCommandRequest.builder()
				.name(getName())
                .description("Fuck You")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("who")
                        .description("Fuck Who?")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .build();
	}

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {
		String who = getOptionAsString(event, "who");

		if (who == null) {
			LOGGER.warn("'who' was null");
			return Mono.empty();
		}

		switch (who) {
		case "you":
		case "u":
			return event.reply(NO_YOU_REPLY);
		case "hazeluff":
		case "hazel":
		case "haz":
			return event.reply(HAZELUFF_REPLY);
		default:
			break;
		}

		// Is a mention
		if (who.startsWith("<@") && who.endsWith(">")) {
			return event.reply(buildDontAtReply(event.getInteraction().getUser()));
		}

		Map<String, List<String>> responses = loadResponsesFromCollection();
		if (responses.containsKey(who)) {
			return event.reply(Utils.getRandom(responses.get(who)));
		}
		
		return event.replyEphemeral(DONT_BE_RUDE);
	}

	static final String NO_YOU_REPLY = "No U.";
	static final String HAZELUFF_REPLY = "Hazeluff doesn't give a fuck.";
	static final String DONT_BE_RUDE = "Don't be rude, please.";

	static String buildDontAtReply(User user) {
		return user.getMention() + ". Don't @ people, you dingus.";
	}

	void add(String subject, String response) {
		subject = subject.toLowerCase();
		Map<String, List<String>> responses = loadResponsesFromCollection();
		if(!responses.containsKey(subject)) {
			responses.put(subject, new ArrayList<>());
		}
		responses.get(subject).add(response);

		saveToCollection(subject, responses.get(subject));
	}

	private Map<String, List<String>> loadResponsesFromCollection() {
		return nhlBot.getPersistentData().getFucksData().getFucks();
	}

	void saveToCollection(String subject, List<String> subjectResponses) {
		nhlBot.getPersistentData().getFucksData().saveToFuckSubjectResponses(subject, subjectResponses);
	}
}
