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

	public static String getCustomMessage(GoalEvent goalEvent) {
		List<CustomMessage> applicableMessages = customMessages.stream()
				.filter(customMsg -> customMsg.getIsApplicable().test(goalEvent))
				.collect(Collectors.toList());
		int index = goalEvent.getId() % applicableMessages.size();
		return applicableMessages.get(index).getMessage();
	}
}
