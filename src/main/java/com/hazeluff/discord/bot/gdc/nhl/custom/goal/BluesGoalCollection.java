package com.hazeluff.discord.bot.gdc.nhl.custom.goal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class BluesGoalCollection extends CustomGoalMessage.Collection {
	public BluesGoalCollection() {
		super();

		/*
		 * Players
		 */
		Map<Integer, List<String>> playerGoalMessages = new HashMap<>(); // Used to shorten invocation/copy-paste

		// Nick Bjugstad
		playerGoalMessages.put(8475760, 
				Arrays.asList("https://tenor.com/view/bjugstad-gif-5525362910723422205"));
		// Pavel Buchnevich
		playerGoalMessages.put(8477402, 
				Arrays.asList("https://tenor.com/view/buchnevich-gif-17803740975985165255"));
		// Dylan Holloway
		playerGoalMessages.put(8482077, 
				Arrays.asList("https://tenor.com/view/holloway-gif-8289398283689544366"));
		// Mathieu Joseph
		playerGoalMessages.put(8478472,
				Arrays.asList("https://tenor.com/view/joseph-gif-11270989782146517273"));
		// Jake Neighbours
		playerGoalMessages.put(482089,
				Arrays.asList("https://tenor.com/view/neighbours-gif-2282639511415501513"));
		// Brayden Schenn
		playerGoalMessages.put(8475170,
				Arrays.asList("https://tenor.com/view/schenn-gif-4128109457960818488"));
		// Jimmy Snuggerud
		playerGoalMessages.put(8483516,
				Arrays.asList("https://tenor.com/view/snuggerud-gif-3866494042307089357"));
		// Oskar Sundqvist
		playerGoalMessages.put(8476897,
				Arrays.asList("https://tenor.com/view/sundqvist-gif-7945712327453555820"));
		// Pius Suter
		playerGoalMessages.put(8480459,
				Arrays.asList("https://tenor.com/view/sundqvist-gif-7945712327453555820",
						"https://media.discordapp.net/attachments/1184407430159945768/1184967743821328444/GBVi3PSaoAAkXcr.png"));
		// Robert Thomas
		playerGoalMessages.put(8480023,
				Arrays.asList("https://tenor.com/view/thomas-gif-7130910433230333796"));
		// Alexey Toropchenko
		playerGoalMessages.put(8480281,
				Arrays.asList("https://tenor.com/view/toropchenko-gif-12528111856409326433"));
		// Nathan Walker
		playerGoalMessages.put(8477573,
				Arrays.asList("https://tenor.com/view/walker-gif-9139975030476261671"));
		// Philip Broberg
		playerGoalMessages.put(8481598,
				Arrays.asList("https://tenor.com/view/broberg-gif-3520193928493434627"));
		// Justin Faulk
		playerGoalMessages.put(8475753,
				Arrays.asList("https://tenor.com/view/faulk-gif-12211683241438495075"));
		// Cam Fowler
		playerGoalMessages.put(8475764,
				Arrays.asList("https://tenor.com/view/fowler-gif-1486041710428459457"));
		// Matthew Kessel
		playerGoalMessages.put(8482516,
				Arrays.asList("https://tenor.com/view/matthew-kessel-gif-18446658632925918338"));
		// Colton Parayko
		playerGoalMessages.put(8476892,
				Arrays.asList("https://tenor.com/view/parayko-gif-4701748441264015002"));
		// Tyler Tucker
		playerGoalMessages.put(8481006,
				Arrays.asList("https://tenor.com/view/tucker-gif-7544705585952294586"));
		// Jordan Kyrou
		playerGoalMessages.put(8479385,
				Arrays.asList("https://tenor.com/view/kyrou-gif-17036790616159111110"));
		

		// register player goal messages
		registerScorers(playerGoalMessages);

	}
}
