package com.hazeluff.ahl.game.event;

import org.bson.BsonDocument;

public class GameEvent {
	protected final BsonDocument jsonEvent;

	protected GameEvent(BsonDocument jsonEvent) {
		this.jsonEvent = jsonEvent;
	}

	protected GameEvent(GameEvent jsonEvent) {
		this.jsonEvent = jsonEvent.jsonEvent;
	}

	public static GameEvent parse(BsonDocument rawJson) {
		GameEvent event = new GameEvent(rawJson);
		if (event.getType() == null) {
			return event;
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
		return this.jsonEvent;
	}

	protected BsonDocument getDetails() {
		return getJson().getDocument("details");
	}
	
	public EventType getType() {
		return EventType.parse(getJson().getString("event").getValue());
	}
}
