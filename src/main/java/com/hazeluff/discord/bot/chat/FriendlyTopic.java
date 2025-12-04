package com.hazeluff.discord.bot.chat;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.utils.Utils;

import discord4j.core.event.domain.message.MessageCreateEvent;


public class FriendlyTopic extends Topic {

	public FriendlyTopic(NHLBot nhlBot) {
		super(nhlBot);
	}

	private static final List<String> replies = Arrays.asList(
			"I hope you have a %s day.",
			"May peace be with you.",
			"Hi There. :kissing_heart:",
			"Hey, How you doin'? :wink:",
			"Bonjour",
			"こんにちは",
			"Hiya!",
			"Hi, How's your day?",
			"I'm glad you noticed me. :D", 
			"Hi there!",
			"Hi! Have you heard of the Church of Pettersson?",
			"Hey now, you're a Brockstar. Get your skates on. Go play!",
			"https://www.youtube.com/watch?v=ZZ5LpwO-An4", // HEYYEYAAEYAAAEYAEYAA
			""
	);

	private static final List<String> niceAdjectives = Arrays.asList(
			"nice",
			"wonderful",
			"terrific",
			"spectacular",
			"radical",
			"spledid",
			"cromulent",
			"awesome",
			"ausgezeichnet",
			"excellent",
			"beautiful"
	);

	@Override
	public void execute(MessageCreateEvent event) {
		int randIdx = Utils.getRandomInt(replies.size());
		String reply = replies.get(randIdx);
		switch(randIdx)
		{
		case 0:
			reply = String.format(reply, Utils.getRandom(niceAdjectives));
			break;
		}
		sendMessage(event, reply);
	}

	@Override
	public boolean isReplyTo(MessageCreateEvent event) {
		return isStringMatch(
				Pattern.compile("\\b(hi|hello|hey|heya|hiya|yo)\\b"),
				event.getMessage().getContent().toLowerCase());
	}

}
