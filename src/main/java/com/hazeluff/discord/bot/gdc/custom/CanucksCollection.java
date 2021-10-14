package com.hazeluff.discord.bot.gdc.custom;

import static com.hazeluff.discord.bot.gdc.custom.CustomMessage.hatTrick;
import static com.hazeluff.discord.bot.gdc.custom.CustomMessage.involved;
import static com.hazeluff.discord.bot.gdc.custom.CustomMessage.scorer;

@SuppressWarnings("serial")
public class CanucksCollection extends CustomMessageCollection {
	public CanucksCollection() {
		super();
		/*
		 * Players
		 */
		// Bo Horvat
		add(new CustomMessage("Hotdog Horvat 🌭", 
				scorer(8477500)));
		add(new CustomMessage("Oh Captain, My Captain!", 
				scorer(8477500)));
		// Elias Pettersson
		add(new CustomMessage("Pistol Pete! 🔫", 
				scorer(8480012)));
		add(new CustomMessage("Petey!", 
				scorer(8480012)));
		// Brock Boeser
		add(new CustomMessage("Brock Hard", 
				scorer(8478444)));	
		add(new CustomMessage("https://www.youtube.com/watch?v=vjheiAQbhQw", null,
				hatTrick(8478444)));
		// J.T. Miller
		add(new CustomMessage("Miller Time!", 
				scorer(8476468)));
		// Nils Hoglander
		add(new CustomMessage("https://tenor.com/view/lion-king-pumba-hunting-bugs-gif-8591753", 
				scorer(8481535)));
		add(new CustomMessage("https://tenor.com/view/razorback-hog-pig-riding-wild-pig-running-gif-17002611",
				involved(8481535)));
		// Connor Garland
		add(new CustomMessage("JAWS! 🦈", 
				scorer(8480012)));
		// Quinne Huges
		add(new CustomMessage("Huggy Bear! 🧸", 
				scorer(8480800)));
		// Jack Rathbone
		add(new CustomMessage("We've got a Boner. 🦴", 
				scorer(8480056)));
		
		/*
		 *  Combos
		 */
		add(new CustomMessage("🇸🇪 🇸🇪 Den svenska touchen! 🇸🇪 🇸🇪",
				involved(8481535, 8480012, 8475171))); // Hog, Petey, OEL
		add(new CustomMessage("LOTTO 6 - 40 - 9 🎫",
				involved(8478444, 8480012, 8476468))); // Brock, Petey, JT
	}
}
