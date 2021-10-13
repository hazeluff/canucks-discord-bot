package com.hazeluff.discord.bot.gdc.custom;

import static com.hazeluff.discord.bot.gdc.custom.CustomMessage.scorer;

@SuppressWarnings("serial")
public class CanucksAlumniCollection extends CustomMessageCollection {
	public CanucksAlumniCollection() {
		super();
		// Loui Eriksson
		add(new CustomMessage("ELITE!!!", scorer(8470626)));
		// Chris Tanev
		add(new CustomMessage("Pug Dad!", scorer(8475690)));
		add(new CustomMessage("Who's our Daddy? D:", scorer(8475690)));
		// Nate Schmidt
		add(new CustomMessage(":schmidtMug:", scorer(8475690)));
	}
}
