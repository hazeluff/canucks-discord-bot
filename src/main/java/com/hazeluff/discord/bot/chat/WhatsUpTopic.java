package com.hazeluff.discord.bot.chat;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.utils.Utils;

import discord4j.core.event.domain.message.MessageCreateEvent;

public class WhatsUpTopic extends Topic {

	public WhatsUpTopic(NHLBot nhlBot) {
		super(nhlBot);
	}

	private static final List<String> replies = Arrays.asList(
		"Feeling: `%s`", // good
		"Watching: %s",
		"Playing: %s",
		"Listening: %s",
		"Nothing Much. You?",
		"Bot stuff. You?",
		"Chillin. Want to join?",
		"Listening to some music.",
		"nm, u?",
		"Nothing much.",
		"Lots of work.",
		"Sup, boss.",
		"Processing stuff...",
		"Have you done all your work?", "Did you forget to do something?",
		"https://www.youtube.com/watch?v=ZZ5LpwO-An4&pp=ygUJaGVtYW4gaGV5", // Heman Heeeey
		"The ceiling.",
		"Fraud ~~tomorrow~~ [OUT NOW!](http://devilmayquake.com/)"
	);

	private static final List<String> feelings = Arrays.asList("weird", "cool", "strange", "godly", "unstopable",
		"tragic", "melancholy", "juicy", "crabby", "bullish", "bearish", "mankey", "stoned", "juiced", "cooked",
		"hungry", "cromulent", "flippant"
	);

	private static final List<String> watchList = Arrays.asList(
		// Movies
		"The Lego Movie (2014)", "The Lego Movie 2: The Second Part (2019)",
		"Star Wars: Episode I - The Phantom Menace (1999)", "Star Wars: Episode II - Attack of the Clones (2002)",
		"Star Wars: Episode III – Revenge of the Sith (2005)", "Pokémon: The First Movie (1998)",
		"Mobile Suit Gundam: Char's Counterattack (1988)", "Mary Poppins (1964)", "Mulan (1994)", "Inside Out (2015)",
		"Shaolin Soccer (2001)",
		
		// TV
		"Full Metal Alchemist", "Lucky Star", "Gosick", "South Park", "The Simpsons",
		
		// YT
		"ElectroBOOM", "Smarter Everyday", "MD Fish Tanks", "4am Laundry", "Retro Game Corps", "EthosLab",
		"pokapoka slime",

		// Vids
		"https://www.youtube.com/watch?v=IfOugI2ccUU" // NoobFromUA - TI3
	);

	private static final List<String> playList = Arrays.asList(
		// Video Games
		"Age of Empires II", "Dota2", "World of Warcraft", "Starcraft 2", "Warcraft 3", "Pokémon: Emerald",
		"Pokémon: Blue", "Pokémon: Crystal", "Pokémon Pearl", "Pokémon: X", "Pokémon: Black", "Pokémon: Black 2",
		"Pokémon: Sun", "Pokémon: Sword", "Pokémon: Violet", "Pokémon: Let's Go, Eevee!", "Puzzle & Dragons",
		"Goof Troops", "XI [sai]", "ULTRAKILL"
	);

	private static final List<String> listenList = Arrays.asList(
		// Music
		"https://www.youtube.com/watch?v=RRKJiM9Njr8", // My Chemical Romance - Welcome To The Black Parade
		"https://www.youtube.com/watch?v=Yiy-plVqG64", // Vylet Pony - NI4NI
		"https://www.youtube.com/watch?v=QDYUiCPLtxk&list=OLAK5uy_loy3XGtnVs4ZEcZUmwVFbGGHUuG9ekBhA", // UK Fraud OST
		"https://www.youtube.com/watch?v=ZaK9Wi5ho0o", // Eminem - Temporary (feat. Skylar Grey)
		"https://www.youtube.com/watch?v=9lZ1EVyVLLo", // Tayla Parx - I Want You
		"https://www.youtube.com/watch?v=2UltvQ1nkuM", // 和你飛
		"https://www.youtube.com/watch?v=6B67YpRsmtw", // Rie Fu - Life is Like a Boat
		"https://www.youtube.com/watch?v=1EzTosXoaN8", // Rie Fu - I Wanna Go To A Place...
		"https://www.youtube.com/watch?v=qu_FSptjRic", // BEYOND - 海闊天空
		"https://www.youtube.com/watch?v=ZSBnCOGj2t4", // ばかみたい
		"https://www.youtube.com/watch?v=T0fy4QEyDPc", // 俺の忘れ物
		"https://www.youtube.com/watch?v=9U3oPEv8gVQ", // Bob's Burgers - Twinkly Lights
		"https://www.youtube.com/watch?v=kQrLZp-BKVw", // High and Mighty Color - Ichirin no Hana 一輪の花
		"https://www.youtube.com/watch?v=BvYuf4r-8xk", // Home Made Kazoku - Thank You!!
		"https://www.youtube.com/watch?v=PPtlmx5dITA", // CHEMISTRY - Change The World
		"https://www.youtube.com/watch?v=ZSWeurc1yMw", // 任賢齊 - 心太軟
		"https://www.youtube.com/watch?v=MF8kHWWFO10", // seven oops - Lovers
		"https://www.youtube.com/watch?v=yfoUZl7Y8TU" // Linkin Park - Meteora
	);
	
	@Override
	public void execute(MessageCreateEvent event) {
		int randIdx = Utils.getRandomInt(replies.size());
		String reply = replies.get(randIdx);
		switch (randIdx) {
		case 0:
			reply = String.format(reply, Utils.getRandom(feelings));
			break;
		case 1:
			reply = String.format(reply, Utils.getRandom(watchList));
			break;
		case 2:
			reply = String.format(reply, Utils.getRandom(playList));
			break;
		case 3:
			reply = String.format(reply, Utils.getRandom(listenList));
			break;
		}
		sendMessage(event, reply);
	}

	@Override
	public boolean isReplyTo(MessageCreateEvent event) {
		return isStringMatch(
				Pattern.compile("\\b((what(')?s\\s*up)|whaddup|wassup|sup)\\b"),
				event.getMessage().getContent().toLowerCase());
	}

}
