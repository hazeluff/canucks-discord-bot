package com.hazeluff.discord.bot.gdc.nhl.custom.goal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.hazeluff.nhl.game.event.GoalEvent;

public class CustomGoalMessages {

	private static final List<CustomGoalMessage> customMessages = 
			Stream.of(
				new CanucksGoalCollection()
			)
            .flatMap(Collection::stream)
					.collect(Collectors.toList());

	public static String getMessage(List<GoalEvent> allGoalEvents, GoalEvent currentEvent) {
		// Filter all GoalEvents down to events that are or were before currentEvent
		List<GoalEvent> previousEvents = allGoalEvents.stream()
				.filter(goalEvent -> goalEvent.getId() <= currentEvent.getId())
				.collect(Collectors.toList());

		// Get all applicable custom messages
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
