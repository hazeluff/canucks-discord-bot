package com.hazeluff.ahl.game.event;

import org.bson.BsonDocument;

public class GoalEvent extends GameEvent {
	public GoalEvent(BsonDocument jsonEvent) {
		super(jsonEvent);
	}

	protected GoalEvent(GameEvent gameEvent) {
		super(gameEvent);
	}
	
	public GoalDetails getGoalDetails() {
		return GoalDetails.parse(getDetails());
	}

	@Override
	public String toString() {
		return String.format("GoalEvent [getGoalDetails()=%s, getType()=%s]", getGoalDetails(), getType());
	}
}
