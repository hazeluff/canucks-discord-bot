package com.hazeluff.discord.bot.chat;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.utils.Utils;

import discord4j.core.event.domain.message.MessageCreateEvent;

public class LovelyTopic extends Topic {
	
	public LovelyTopic(NHLBot nhlBot) {
		super(nhlBot);
	}

	private static final List<String> replies = Arrays.asList(
			"I %s %s.",
			"Take me out to dinner? :%s:",
			"How about drinks? :%s:",
			"Love you too.",
			"<3",
			"Baka",
			"UwU",
			":blush:",
			":wink:",
			"I think it's better we stay friends...",
			":heart_eyes:",
			":heart_eyes_cat:",
			"Thanks.",
			"Thank you, come again.",
			"b-b-b-baka",
			"This will not buy you favors with the mods.",
			"Meet you by the dumpsters behind Wendy's.",
			"If you want to be my lover, you gotta get with my friends.",
			"I'm just a clanker."			
	);
	
	private static final List<String> waysToLike = Arrays.asList(
			"like", "love", "enjoy", "am a enjoyer of", "appreciate"
	);
	
	private static final List<String> thingsToLike = Arrays.asList(
			"everyone", "Hazeluff", "turtles", "cats", "dogs", "food", "cooking", "sewing", "money", "Pokemon",
			"Magic: The Gathering", "YuGiOh", "Canada", "hockey", "Green Day", "Linkin Park", "Eminem", "Hatsune Miku",
			"freedom", "GorillaZ", "Wu Tang Clan", "Bahn Mi", "Star Kebab", "Dick's", "going to Japan", "video games",
			"Minecraft", "UltraKill", "Dota2", "Starcraft", "Warcraft", "onii-chan", "onee-chan", "the cool shoe shine",
			"pumped up kicks", "WoW", "420", "69", "Batman", "Spiderman", "The Spirit"
	);

	private static final List<String> foodEmojis = Arrays.asList(
			"hotdog", "hamburger", "fries", "ramen", "dogs", "bagel", "pancake", "pizza", "stuffed_pita", "burrito",
			"onigiri", "curry", "sushi", "spaghetti", "icecream", "cake", "donut", "takeout_box", "bento",
			"meat_on_bone"
	);

	private static final List<String> drinkEmojis = Arrays.asList(
			"beer", "bubble_tea", "whiskey", "champagne", "sake"
	);
	
	@Override
	public void execute(MessageCreateEvent event) {
		int randIdx = Utils.getRandomInt(replies.size());
		String reply = replies.get(randIdx);
		switch(randIdx)
		{
		case 0:
			reply = String.format(reply, Utils.getRandom(waysToLike), Utils.getRandom(thingsToLike));
			break;
		case 1:
			reply = String.format(reply, Utils.getRandom(foodEmojis));
			break;
		case 2:
			reply = String.format(reply, Utils.getRandom(drinkEmojis));
			break;
		}
		sendMessage(event, reply);
	}

	@Override
	public boolean isReplyTo(MessageCreateEvent event) {
		return isStringMatch(
				Pattern.compile("(\\bi\\s*(love|like)\\s*(u|you)\\b)|\\bilu\\b|:kiss:|:kissing:|:heart:|<3"),
				event.getMessage().getContent().toLowerCase());
	}

}
