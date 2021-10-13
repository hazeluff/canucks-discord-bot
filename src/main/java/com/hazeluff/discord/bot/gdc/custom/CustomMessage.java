package com.hazeluff.discord.bot.gdc.custom;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.hazeluff.nhl.event.GoalEvent;

public class CustomMessage {

	private final String message;
	private final Predicate<GoalEvent> isApplicable;

	public CustomMessage(String message, Predicate<GoalEvent> isApplicable) {
		this.message = message;
		this.isApplicable = isApplicable;
	}

	public String getMessage() {
		return message;
	}

	public Predicate<GoalEvent> getIsApplicable() {
		return isApplicable;
	}

	/*
	 * Convenient Predicates
	 */
	public static Predicate<GoalEvent> isScorer(int playerId) {
		return goalEvent -> goalEvent.getPlayers().get(0).getId() == playerId;
	}

	public static Predicate<GoalEvent> isInvolved(int playerId) {
		return goalEvent -> goalEvent.getPlayers().stream().anyMatch(player -> player.getId() == playerId);
	}

	public static Predicate<GoalEvent> isInvolved(Integer... playerIds) {
		List<Integer> playerIdList = Arrays.asList(playerIds);
		return goalEvent -> goalEvent.getPlayers().stream().allMatch(player -> playerIdList.contains(player.getId()));
	}
}
