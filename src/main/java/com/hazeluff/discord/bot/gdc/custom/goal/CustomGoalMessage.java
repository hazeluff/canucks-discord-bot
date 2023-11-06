package com.hazeluff.discord.bot.gdc.custom.goal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.hazeluff.nhl.Team;
import com.hazeluff.nhl.event.GoalEvent;

public class CustomGoalMessage {

	private final String message;
	private final Predicate<GoalEvent> goalApplicable;
	private final Predicate<List<GoalEvent>> goalsApplicable;
	private final int priority;

	public CustomGoalMessage(String message, Predicate<GoalEvent> goalApplicable,
			Predicate<List<GoalEvent>> goalsApplicable, int priority) {
		this.message = message;
		this.goalApplicable = goalApplicable;
		this.goalsApplicable = goalsApplicable;
		this.priority = priority;
	}

	public CustomGoalMessage(String message, Predicate<GoalEvent> goalApplicable, int priority) {
		this(message, goalApplicable, null, priority);
	}

	public String getMessage() {
		return message;
	}

	public int getPriority() {
		return priority;
	}

	public boolean applies(List<GoalEvent> previousEvents, GoalEvent currentEvent) {
		if (currentEvent == null || previousEvents == null) {
			return false;
		}
		if (goalApplicable != null && !goalApplicable.test(currentEvent)) {
			return false;
		}
		if (goalsApplicable != null && !goalsApplicable.test(previousEvents)) {
			return false;
		}
		return true;
	}

	/*
	 * Convenient Instance Creators
	 */
	public static CustomGoalMessage goals(String message, int priority, int playerId, int numGoals) {
		return new CustomGoalMessage(
				message, 
				goalEvent -> goalEvent.getScorerId() == playerId, 
				goalEvents -> {
					long goals = goalEvents.stream().filter(goal -> goal.getScorerId() == playerId).count();
					return goals == numGoals;
				}, 
				priority);
	}
	
	public static CustomGoalMessage hatTrick(String message, int playerId) {
		return goals(message, 3, playerId, 3);
	}
	
	public static CustomGoalMessage scorer(String message, int scoringPlayerId) {
		return new CustomGoalMessage(
				message, 
				goalEvent -> goalEvent.getScorerId() == scoringPlayerId, 
				2);
	}

	public static CustomGoalMessage involved(String message, int involvedPlayerId) {
		return new CustomGoalMessage(
				message, 
				goalEvent -> goalEvent.getPlayerIds().contains(involvedPlayerId), 
				2);
	}

	public static CustomGoalMessage involved(String message, Integer... playerIds) {
		List<Integer> involvedPlayerIdList = Arrays.asList(playerIds);
		return new CustomGoalMessage(message,
				goalEvent -> goalEvent.getPlayerIds().containsAll(involvedPlayerIdList),
				2);
	}

	public static CustomGoalMessage team(String message, Team team) {
		return new CustomGoalMessage(message,
				goalEvent -> goalEvent.getTeam().equals(team),
				1);
	}

	@SuppressWarnings("serial")
	public static abstract class Collection extends ArrayList<CustomGoalMessage> {

	}

}
