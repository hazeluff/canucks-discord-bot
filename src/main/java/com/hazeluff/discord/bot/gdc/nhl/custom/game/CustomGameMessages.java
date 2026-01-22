package com.hazeluff.discord.bot.gdc.nhl.custom.game;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.hazeluff.discord.nhl.NHLTeams.Team;
import com.hazeluff.nhl.game.Game;

public class CustomGameMessages {
	private static final Map<Team, List<CustomGameMessage>> startGameMessagesMap = new HashMap<Team, List<CustomGameMessage>>() {{
		put(Team.VANCOUVER_CANUCKS, new CanucksStartGameCollection());
	}};

	private static final Map<Team, List<CustomGameMessage>> endGameMessagesMap = new HashMap<Team, List<CustomGameMessage>>() {{
		put(Team.VANCOUVER_CANUCKS, new CanucksEndGameCollection());
	}};

	public static String getStartGameMessage(Game game, Team team) {
		return getCustomMessage(game, startGameMessagesMap.get(team));
	}

	public static String getEndGameMessage(Game game, Team team) {
		return getCustomMessage(game, endGameMessagesMap.get(team));
	}

	public static String getCustomMessage(Game game, List<CustomGameMessage> messages) {
		if(messages == null) {
			return null;
		}
		
		// Get all applicable messages. If no custom ones exist, get from the generic list.
		List<CustomGameMessage> applicableMessages = messages.stream()
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
