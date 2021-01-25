package com.hazeluff.discord.bot.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang.StringUtils;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.utils.Utils;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.MessageCreateSpec;

/**
 * Because fuck Mark Messier
 */
public class FuckCommand extends Command {

	static final Consumer<MessageCreateSpec> NOT_ENOUGH_PARAMETERS_REPLY = spec -> spec
			.setContent("You're gonna have to tell me who/what to fuck. `?fuck [thing]`");
	static final Consumer<MessageCreateSpec> NO_YOU_REPLY = spec -> spec
			.setContent("No U.");
	static final Consumer<MessageCreateSpec> HAZELUFF_REPLY = spec -> spec
			.setContent("Hazeluff doesn't give a fuck.");

	public FuckCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	@Override
	public void execute(MessageCreateEvent event, CommandArguments command) {
		
		if (command.getArguments().isEmpty()) {
			sendMessage(event, NOT_ENOUGH_PARAMETERS_REPLY);
			return;
		}
		
		if (command.getArguments().get(0).toLowerCase().equals("you")
				|| command.getArguments().get(0).toLowerCase().equals("u")) {
			sendMessage(event, NO_YOU_REPLY);
			return;
		}
		
		if (command.getArguments().get(0).toLowerCase().equals("hazeluff")
				|| command.getArguments().get(0).toLowerCase().equals("hazel")
				|| command.getArguments().get(0).toLowerCase().equals("haze")
				|| command.getArguments().get(0).toLowerCase().equals("haz")) {
			sendMessage(event, HAZELUFF_REPLY);
			return;
		}

		if (command.getArguments().get(0).startsWith("<@") && command.getArguments().get(0).endsWith(">")) {
			nhlBot.getDiscordManager().deleteMessage(event.getMessage());
			sendMessage(event, buildDontAtReply(event.getMessage()));
			return;
		}

		if (command.getArguments().get(0).toLowerCase().equals("add")) {
			User author = event.getMessage().getAuthor().orElse(null);
			if (author != null && isDev(author.getId())) {
				String subject = command.getArguments().get(1);
				List<String> response = new ArrayList<>(command.getArguments());
				String strResponse = StringUtils.join(response.subList(3, response.size()), " ");				
				add(subject, strResponse);
				sendMessage(event, spec -> spec.setContent(strResponse));
			}
			return;
		}

		Map<String, List<String>> responses = loadResponsesFromCollection();
		if (responses.containsKey(command.getArguments().get(0))) {
			sendMessage(event, spec -> spec.setContent(Utils.getRandom(responses.get(command.getArguments().get(0)))));
			return;
		}
	}

	static Consumer<MessageCreateSpec> buildDontAtReply(Message message) {
		String authorMention = String.format("<@%s>", message.getAuthor().get());
		return spec -> spec.setContent(authorMention + ". Don't @ people, you dingus.");
	}

	static Consumer<MessageCreateSpec> buildAddReply(String subject, String response) {
		return spec -> spec.setContent(
				String.format("Added new response.\nSubject: `%s`\nResponse: `%s`", subject.toLowerCase(), response));
	}

	@Override
	public boolean isAccept(Message message, CommandArguments command) {
		return command.getCommand().equalsIgnoreCase("fuck");
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
