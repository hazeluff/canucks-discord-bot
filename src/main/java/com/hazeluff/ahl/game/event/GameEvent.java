package com.hazeluff.ahl.game.event;

import org.bson.BsonDocument;

public class GameEvent {
	protected final BsonDocument jsonWrapper;
	protected final EventType type;

	protected GameEvent(BsonDocument jsonWrapper) {
		this.jsonWrapper = jsonWrapper;
		this.type = EventType.parse(jsonWrapper.getString("event").getValue());
	}

	protected GameEvent(GameEvent event) {
		this.jsonWrapper = event.jsonWrapper;
		this.type = event.type;
	}

	public static GameEvent parse(BsonDocument jsonWrapper) {
		GameEvent event = new GameEvent(jsonWrapper);
		if (event.getType() == null) {
			// Unknown EventType
			return null;
		}
		switch (event.getType()) {
		case GOAL:
			return new GoalEvent(event);
		case PENALTY:
			return new PenaltyEvent(event);
		case SHOOTOUT:
			return new ShootoutEvent(event);
		default:
			return event;
		}
	}

	protected BsonDocument getJson() {
		return this.jsonWrapper;
	}

	protected BsonDocument getDetails() {
		return getJson().getDocument("details");
	}
	
	public EventType getType() {
		return EventType.parse(getJson().getString("event").getValue());
	}
}
