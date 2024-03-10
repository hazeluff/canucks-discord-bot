package com.hazeluff.nhl.event;

import java.util.concurrent.atomic.AtomicReference;

import org.bson.BsonDocument;

import com.hazeluff.nhl.game.EventType;
import com.hazeluff.nhl.game.PeriodType;

public class GameEvent {
	protected AtomicReference<BsonDocument> jsonEvent;

	private GameEvent(BsonDocument jsonEvent) {
		this.jsonEvent = new AtomicReference<>(jsonEvent);
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
		default:
			return event;
		}
	}

	public BsonDocument getJson() {
		return this.jsonEvent.get();
	}

	public EventType getType() {
		return EventType.parse(getJson().getString("typeDescKey").getValue());
	}

	public int getId() {
		return getJson().getInt32("eventId").getValue();
	}

	public int getPeriod() {
		return getJson().getDocument("periodDescriptor").getInt32("number").getValue();
	}

	public PeriodType getPeriodType() {
		return PeriodType.parse(getJson().getDocument("periodDescriptor").getString("periodType").getValue());
	}

	public String getPeriodTime() {
		return getJson().getString("timeInPeriod").getValue();
	}

	/**
	 * Situation on ice. i.e. number of skaters and goalies for each team.
	 * 
	 * @return Format: "ABYZ"
	 *         <ul>
	 *         <li>A: Home Goalie (1 in, 0 pulled)</li>
	 *         <li>B: # of Home Skaters</li>
	 *         <li>Y: # of Away Skaters</li>
	 *         <li>Z: Away Goalie (1 in, 0 pulled)</li>
	 *         </ul>
	 */
	public String getSituationCode() {
		return getJson().getString("situationCode").getValue();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jsonEvent == null) ? 0 : jsonEvent.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GameEvent other = (GameEvent) obj;
		if (jsonEvent == null) {
			if (other.jsonEvent != null)
				return false;
		} else if (!jsonEvent.equals(other.jsonEvent))
			return false;
		return true;
	}
}
