package com.hazeluff.discord.bot.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.chat.FriendlyTopic;
import com.hazeluff.discord.bot.chat.LovelyTopic;
import com.hazeluff.discord.bot.chat.RudeTopic;
import com.hazeluff.discord.bot.chat.Topic;
import com.hazeluff.discord.bot.chat.WhatsUpTopic;
import com.hazeluff.discord.utils.UserThrottler;
import com.hazeluff.discord.utils.Utils;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;

public class MessageListener extends EventListener {
	static final String UNKNOWN_COMMAND_MESSAGE = 
			"Sorry, I don't understand that. Send `@NHLBot help` for a list of commands.";
	static final String FUCK_MESSIER_MESSAGE = "FUCK MESSIER";
	static long FUCK_MESSIER_COUNT_LIFESPAN = 60000;

	private final List<Topic> topics;

	private final UserThrottler userThrottler;

	public MessageListener(NHLBot nhlBot) {
		super(nhlBot);

		topics = new ArrayList<>();
		topics.add(new FriendlyTopic(nhlBot));
		topics.add(new LovelyTopic(nhlBot));
		topics.add(new RudeTopic(nhlBot));
		topics.add(new WhatsUpTopic(nhlBot));

		userThrottler = new UserThrottler();
	}

	/**
	 * For Tests
	 */
	MessageListener(NHLBot nhlBot, List<Topic> topics, UserThrottler userThrottler) {
		super(nhlBot);
		this.topics = topics;
		this.userThrottler = userThrottler;
	}

	@Override
	public void processEvent(Event event) {
		if (event instanceof MessageCreateEvent) {
			processEvent((MessageCreateEvent) event);
		}
	}

	/**
	 * Gets a specification for the message to reply with.
	 */
	public void processEvent(MessageCreateEvent event) {
		User author = event.getMessage().getAuthor().orElse(null);
		if (author == null || author.getId().equals(getNHLBot().getDiscordManager().getId())) {
			return;
		}

		Snowflake authorId = author.getId();

		userThrottler.add(authorId);

		if (userThrottler.isThrottle(authorId)) {
			return;
		}
		
		Snowflake guildId = event.getGuildId().orElse(null);
		if (guildId == null) {
			return;
		}

		if (replyToMention(event)) {
			return;
		}
	}

	/**
	 * Gets the specification for the reply message for if the NHLBot is
	 * mentioned and phrases match ones that have responses.
	 * 
	 * @param event
	 *            event that we are replying to
	 * @return true if mention topic is found and excuted
	 */
	boolean replyToMention(MessageCreateEvent event) {

		if (isBotMentioned(event)) {
			Optional<Topic> matchedCommand = topics.stream().filter(topic -> topic.isReplyTo(event)).findFirst();
			if (matchedCommand.isPresent()) {
				matchedCommand.get().execute(event);
				return true;
			}
		}

		return false;
	}

	/**
	 * Determines if NHLBot is mentioned in the message.
	 * 
	 * @param message
	 *            message to determine if NHLBot is mentioned
	 * @return true, if NHL Bot is mentioned.<br>
	 *         false, otherwise.
	 */
	boolean isBotMentioned(MessageCreateEvent event) {
		return event.getMessage().getUserMentionIds().contains(getNHLBot().getDiscordManager().getId());
	}

	long getCurrentTime() {
		return Utils.getCurrentTime();
	}
}
