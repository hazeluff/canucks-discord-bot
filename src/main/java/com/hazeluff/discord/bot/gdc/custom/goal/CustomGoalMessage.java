package com.hazeluff.discord.bot.gdc.custom.goal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.hazeluff.discord.nhl.NHLTeams.Team;
import com.hazeluff.nhl.game.event.GoalEvent;

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

	@SuppressWarnings("serial")
	public static abstract class Collection extends ArrayList<CustomGoalMessage> {
		abstract Team getTeam();

		/*
		 * Convenient List Appenders
		 */
		void goals(String message, int priority, int playerId, int numGoals) {
			CustomGoalMessage customMessage = new CustomGoalMessage(
					message, 
					goalEvent -> goalEvent.getScorerId() == playerId, 
					goalEvents -> {
						long goals = goalEvents.stream().filter(goal -> goal.getScorerId() == playerId).count();
						return goals == numGoals;
					}, 
					priority);
			add(customMessage);
		}

		void hatTrick(String message, int playerId) {
			goals(message, 3, playerId, 3);
		}
		
		void scorer(String message, int scoringPlayerId) {
			CustomGoalMessage customMessage = new CustomGoalMessage(
					message, 
					goalEvent -> goalEvent.getScorerId() == scoringPlayerId, 
					2);
			add(customMessage);
		}

		void involved(String message, int involvedPlayerId) {
			CustomGoalMessage customMessage = new CustomGoalMessage(message,
					goalEvent -> goalEvent.getPlayerIds().contains(involvedPlayerId), 2);
			add(customMessage);
		}

		void involved(String message, Integer... playerIds) {
			List<Integer> involvedPlayerIdList = Arrays.asList(playerIds);
			CustomGoalMessage customMessage =  new CustomGoalMessage(message,
					goalEvent -> goalEvent.getPlayerIds().containsAll(involvedPlayerIdList),
					2);
			add(customMessage);
		}

		void team(String message) {
			CustomGoalMessage customMessage =  new CustomGoalMessage(message,
					goalEvent -> goalEvent.getTeam().equals(getTeam()),
					1);
			add(customMessage);
		}
	}

}
