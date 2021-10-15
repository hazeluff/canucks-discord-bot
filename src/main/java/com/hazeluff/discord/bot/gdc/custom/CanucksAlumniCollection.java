package com.hazeluff.discord.bot.gdc.custom;

import static com.hazeluff.discord.bot.gdc.custom.CustomMessage.scorer;

@SuppressWarnings("serial")
public class CanucksAlumniCollection extends CustomMessageCollection {
	public CanucksAlumniCollection() {
		super();
		// Loui Eriksson
		add(new CustomMessage(
				"ELITE!!!",
				scorer(8470626)));
		// Chris Tanev
		add(new CustomMessage(
				"Pug Dad!",
				scorer(8475690)));
		add(new CustomMessage(
				"Who's our Daddy? D:",
				scorer(8475690)));
		add(new CustomMessage(
				"https://giphy.com/gifs/nhl-lol-hair-tanev-WQxrnpiCEiKsZwh8VY",
				scorer(8475690)));
		// Alex Edler
		add(new CustomMessage( // Break dance
				"https://gfycat.com/gregariousunhappyelk",
				scorer(8479355)));
		// Nate Schmidt
		add(new CustomMessage( // :schmidtMug:
				"https://cdn.discordapp.com/emojis/805996907314413578.png",
				scorer(8475690)));
		// Zack MacEwen
		add(new CustomMessage(
				"Big MAC 🍔", 
				scorer(8479772)));
		// Olli Juolevi
		add(new CustomMessage(
				"Olli Not Ded",
				scorer(8479355)));
	}
}
