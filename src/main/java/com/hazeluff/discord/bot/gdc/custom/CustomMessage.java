package com.hazeluff.discord.bot.gdc.custom;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.hazeluff.nhl.Team;
import com.hazeluff.nhl.event.GoalEvent;

public class CustomMessage {

	private final String message;
	private final Predicate<GoalEvent> goalApplicable;
	private final Predicate<List<GoalEvent>> goalsApplicable;

	public CustomMessage(String message, Predicate<GoalEvent> goalApplicable,
			Predicate<List<GoalEvent>> goalsApplicable) {
		this.message = message;
		this.goalApplicable = goalApplicable;
		this.goalsApplicable = goalsApplicable;
	}

	public CustomMessage(String message, Predicate<GoalEvent> goalApplicable) {
		this(message, goalApplicable, null);
	}

	public String getMessage() {
		return message;
	}

	public boolean isApplicable(List<GoalEvent> previousEvents, GoalEvent currentEvent) {
		if (currentEvent == null || previousEvents == null) {
			return false;
		}
		boolean testEvent = goalApplicable == null ? false : goalApplicable.test(currentEvent);
		boolean testEvents = goalsApplicable == null ? false : goalsApplicable.test(previousEvents);
		return testEvent || testEvents;
	}

	/*
	 * Convenient Predicates
	 */
	public static Predicate<GoalEvent> scorer(int playerId) {
		return goalEvent -> goalEvent.getPlayers().get(0).getId() == playerId;
	}

	public static Predicate<List<GoalEvent>> hatTrick(int playerId) {
		return goalEvents -> {
			long numGoals = goalEvents.stream().filter(goal -> goal.getPlayers().get(0).getId() == playerId).count();
			return numGoals == 3;
		};
	}

	public static Predicate<GoalEvent> involved(int playerId) {
		return goalEvent -> goalEvent.getPlayers().stream().anyMatch(player -> player.getId() == playerId);
	}

	public static Predicate<GoalEvent> involved(Integer... playerIds) {
		List<Integer> playerIdList = Arrays.asList(playerIds);
		return goalEvent -> goalEvent.getPlayers().stream().allMatch(player -> playerIdList.contains(player.getId()));
	}

	public static Predicate<GoalEvent> team(Team team) {
		return goalEvent -> goalEvent.getTeam().equals(team);
	}
}
