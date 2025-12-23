package com.hazeluff.discord.bot.chat;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.utils.Utils;

import discord4j.core.event.domain.message.MessageCreateEvent;

public class RudeTopic extends Topic {

	public RudeTopic(NHLBot nhlBot) {
		super(nhlBot);
	}

	private static final List<String> replies = Arrays.asList(
			"ok, %s",
			"Nah, you should fuck off.", 
			"Go ahead and leave the server.", 
			"You can suck my dick.",
			"Go, take it, and shove it up your butt.",
			"Please, eat shit.",
			"Get fucked.",
			"You are cordially invited to get fucked.", 
			"Bleep Bloop. I am just a robot.",
			"You're probably getting coal this Christmas.", 
			"I'm just doing my job. :cry:", 
			"That's not nice.",
			"Hazeluff worked really hard on me.",
			"A moderator will be here shortly to discuss this matter.",
			"You need to have a Positive Mental Attitude.",
			"2 Kings 2:23-24", // Elisha Is Jeered
			"Acts 20:9",
			// "As Paul spoke on and on, a young man named Eutychus,
			// sitting on the windowsill, became very drowsy. Finally,
			// he fell sound asleep and dropped three stories to his death below."
			"Social Credits have been deducted from your profile.",
			"You have been reported to the local authorities.",
			"git gud scrub",
			"Thank you, come again.",
			"Why you hef to be mad? https://www.youtube.com/watch?v=xzpndHtdl9A",
			"Fin would not approve.",
			"?",
			"huh?",
			"no",
			"吓?",
			"係咩?",
			"n00b",
			"|\\\\|()()|3",
			"scrub",
			"lol"
	);
	
	private static final List<String> condescendingToken = Arrays.asList(
			"w/e", "buddy", "pal", "twat", "idiot"
	);
	
	@Override
	public void execute(MessageCreateEvent event) {
		int randIdx = Utils.getRandomInt(replies.size());
		String reply = replies.get(randIdx);
		switch(randIdx)
		{
		case 0:
			reply = String.format(reply, Utils.getRandom(condescendingToken));
			break;
		}
		sendMessage(event, reply);
	}

	@Override
	public boolean isReplyTo(MessageCreateEvent event) {
		return isStringMatch(
				Pattern.compile("\\b((fuck\\s*off)|(shut\\s*(up|it))|(fuck\\s*(you|u)))\\b"),
				event.getMessage().getContent().toLowerCase());
	}

}
