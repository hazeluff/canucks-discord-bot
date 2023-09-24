package com.hazeluff.nhl.event;

import org.bson.BsonDocument;

import com.hazeluff.nhl.game.EventType;

public class GameEvent {
	protected BsonDocument rawJson;

	private GameEvent(BsonDocument rawJson) {
		this.rawJson = rawJson;
	}

	protected GameEvent(GameEvent gameEvent) {
		this.rawJson = gameEvent.rawJson;
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

	public EventType getType() {
		return EventType.parse(rawJson.getString("typeDescKey").getValue());
	}

	public int getId() {
		return rawJson.getInt32("eventId").getValue();
	}

	public int getPeriod() {
		return rawJson.getInt32("period").getValue();
	}

	public String getPeriodTime() {
		return rawJson.getString("timeInPeriod").getValue();
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
		return rawJson.getString("situationCode").getValue();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rawJson == null) ? 0 : rawJson.hashCode());
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
		if (rawJson == null) {
			if (other.rawJson != null)
				return false;
		} else if (!rawJson.equals(other.rawJson))
			return false;
		return true;
	}
}
