package com.hazeluff.discord.bot.gdc.nhl.custom.goal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.hazeluff.discord.nhl.NHLTeams.Team;
import com.hazeluff.nhl.game.event.GoalEvent;

public class CustomGoalMessages {
	private static final Map<Team, List<CustomGoalMessage>> customMessagesMap = 
		new HashMap<Team, List<CustomGoalMessage>>() {{
			put(Team.VANCOUVER_CANUCKS, new CanucksGoalCollection());
		}};

	public static String getMessage(List<GoalEvent> allGoalEvents, GoalEvent currentEvent) {
		// Filter all GoalEvents down to events that are or were before currentEvent
		List<GoalEvent> previousEvents = allGoalEvents.stream()
				.filter(goalEvent -> goalEvent.getId() <= currentEvent.getId())
				.collect(Collectors.toList());

		// Get all applicable custom messages
		Team team = currentEvent.getTeam();
		List<CustomGoalMessage> customMessages = customMessagesMap.get(team);
		if (customMessages == null) {
			return null;
		}
		List<CustomGoalMessage> applicableMessages = customMessages.stream()
				.filter(customMsg -> customMsg.applies(previousEvents, currentEvent))
				.collect(Collectors.toList());
		if (applicableMessages.isEmpty()) {
			return null;
		}

		// Filter list down to only custom messages of highest priority
		int highestMsgPrio = Collections
				.max(applicableMessages.stream().map(CustomGoalMessage::getPriority).collect(Collectors.toList()));
		applicableMessages = applicableMessages.stream()
				.filter(msg -> msg.getPriority() == highestMsgPrio)
				.collect(Collectors.toList());

		int index = currentEvent.getId() % applicableMessages.size();
		return applicableMessages.get(index).getMessage();
	}
}
