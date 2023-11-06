package com.hazeluff.discord.bot.gdc.custom.game;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.hazeluff.nhl.game.Game;

public class CustomGameMessages {

	private static final List<CustomGameMessage> customMessages = 
			Stream.of(
				new CanucksGameCollection()
			)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

	public static String getMessage(Game game) {
		// Get all applicable custom messages
		List<CustomGameMessage> applicableMessages = customMessages.stream()
				.filter(customMsg -> customMsg.applies(game))
				.collect(Collectors.toList());
		if (applicableMessages.isEmpty()) {
			return null;
		}

		// Filter list down to only custom messages of highest priority
		int highestMsgPrio = Collections
				.max(applicableMessages.stream().map(CustomGameMessage::getPriority).collect(Collectors.toList()));
		applicableMessages = applicableMessages.stream()
				.filter(msg -> msg.getPriority() == highestMsgPrio)
				.collect(Collectors.toList());

		int index = game.getGameId() % applicableMessages.size();
		return applicableMessages.get(index).getMessage();
	}
}
