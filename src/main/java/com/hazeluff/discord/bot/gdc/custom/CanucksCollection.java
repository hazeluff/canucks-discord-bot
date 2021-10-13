package com.hazeluff.discord.bot.gdc.custom;

import static com.hazeluff.discord.bot.gdc.custom.CustomMessage.isInvolved;
import static com.hazeluff.discord.bot.gdc.custom.CustomMessage.isScorer;

@SuppressWarnings("serial")
public class CanucksCollection extends CustomMessageCollection {
	public CanucksCollection() {
		super();
		/*
		 * Players
		 */
		// Bo Horvat
		add(new CustomMessage("Hotdog Horvat 🌭", 
				isScorer(8477500)));
		add(new CustomMessage("Oh Captain, My Captain!", 
				isScorer(8477500)));
		// Elias Pettersson
		add(new CustomMessage("Pistol Pete! 🔫", 
				isScorer(8480012)));
		add(new CustomMessage("Petey!", 
				isScorer(8480012)));
		// Brock Boeser
		add(new CustomMessage("Brock Hard", 
				isScorer(8478444)));
		// Nils Hoglander
		add(new CustomMessage("https://tenor.com/view/lion-king-pumba-hunting-bugs-gif-8591753", 
				isScorer(8481535)));
		add(new CustomMessage("https://tenor.com/view/razorback-hog-pig-riding-wild-pig-running-gif-17002611",
				isInvolved(8481535)));
		// Connor Garland
		add(new CustomMessage("JAWS! 🦈", 
				isScorer(8480012)));
		// Zack MacEwen
		add(new CustomMessage("Big MAC 🍔", 
				isScorer(8479772)));
		// Quinne Huges
		add(new CustomMessage("Huggy Bear! 🧸", 
				isScorer(8480800)));
		// Jack Rathbone
		add(new CustomMessage("We've got a Boner. 🦴", 
				isScorer(8480056)));
		
		/*
		 *  Combos
		 */
		add(new CustomMessage("🇸🇪 🇸🇪 Den svenska touchen! 🇸🇪 🇸🇪",
				isInvolved(8481535, 8480012, 8475171))); // Hog, Petey, OEL
	}
}
