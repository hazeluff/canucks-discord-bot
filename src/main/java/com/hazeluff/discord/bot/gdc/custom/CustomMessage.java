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
	private final int priority;

	public CustomMessage(String message, Predicate<GoalEvent> goalApplicable,
			Predicate<List<GoalEvent>> goalsApplicable, int priority) {
		this.message = message;
		this.goalApplicable = goalApplicable;
		this.goalsApplicable = goalsApplicable;
		this.priority = priority;
	}

	public CustomMessage(String message, Predicate<GoalEvent> goalApplicable, int priority) {
		this(message, goalApplicable, null, priority);
	}

	public String getMessage() {
		return message;
	}

	public int getPriority() {
		return priority;
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
	 * Convenient Instance Creators
	 */
	public static CustomMessage hatTrick(String message, int playerId) {
		return new CustomMessage(
				message, 
				null, 
				goalEvents -> {
					long numGoals = goalEvents.stream().filter(goal -> goal.getScorerId() == playerId).count();
					return numGoals == 3;
				}, 
				3);
	}
	
	public static CustomMessage scorer(String message, int scoringPlayerId) {
		return new CustomMessage(
				message, 
				goalEvent -> goalEvent
						.getScorerId() == scoringPlayerId,
				2);
	}

	public static CustomMessage involved(String message, int involvedPlayerId) {
		return new CustomMessage(
				message, 
				goalEvent -> goalEvent.getPlayerIds().stream().anyMatch(
						playerId -> playerId == involvedPlayerId),
				2);
	}

	public static CustomMessage involved(String message, Integer... playerIds) {
		List<Integer> involvedPlayerIdList = Arrays.asList(playerIds);
		return new CustomMessage(message,
				goalEvent -> goalEvent.getPlayerIds().stream()
						.allMatch(playerId -> involvedPlayerIdList.contains(playerId)),
				2);
	}

	public static CustomMessage team(String message, Team team) {
		return new CustomMessage(message,
				goalEvent -> goalEvent.getTeam().equals(team),
				1);
	}
}
