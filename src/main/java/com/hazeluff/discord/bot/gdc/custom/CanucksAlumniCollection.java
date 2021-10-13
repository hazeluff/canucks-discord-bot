package com.hazeluff.discord.bot.gdc.custom;

import static com.hazeluff.discord.bot.gdc.custom.CustomMessage.isScorer;

@SuppressWarnings("serial")
public class CanucksAlumniCollection extends CustomMessageCollection {
	public CanucksAlumniCollection() {
		super();
		// Loui Eriksson
		add(new CustomMessage("ELITE!!!", isScorer(8470626)));
		// Chris Tanev
		add(new CustomMessage("Pug Dad!", isScorer(8475690)));
		add(new CustomMessage("Who's our Daddy? D:", isScorer(8475690)));
		// Nate Schmidt
		add(new CustomMessage(":schmidtMug:", isScorer(8475690)));
	}
}
