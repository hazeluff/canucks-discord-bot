package com.hazeluff.discord.bot.gdc.custom;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.hazeluff.nhl.event.GoalEvent;

public class CustomMessages {

	private static final List<CustomMessage> customMessages = 
			Stream.of(
					new CanucksCollection(), 
					new CanucksAlumniCollection()
			)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());;

	public static String getCustomMessage(List<GoalEvent> allGoalEvents, GoalEvent currentEvent) {
		// Filter allGoalEvents down to events that are or were before currentEvent
		List<GoalEvent> previousEvents = allGoalEvents.stream()
				.filter(goalEvent -> goalEvent.getId() <= currentEvent.getId())
				.collect(Collectors.toList());
		List<CustomMessage> applicableMessages = customMessages.stream()
				.filter(customMsg -> customMsg.isApplicable(previousEvents, currentEvent))
				.collect(Collectors.toList());
		if (applicableMessages.isEmpty()) {
			return null;
		}
		int index = currentEvent.getId() % applicableMessages.size();
		return applicableMessages.get(index).getMessage();
	}
}
