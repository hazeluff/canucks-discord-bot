package com.hazeluff.nhl.event;

import org.bson.BsonDocument;
import org.bson.BsonInt32;

import com.hazeluff.nhl.Team;

public class PenaltyEvent extends GameEvent {
	protected PenaltyEvent(GameEvent gameEvent) {
		super(gameEvent);
	}

	public BsonDocument getDetails() {
		return getJson().getDocument("details");
	}

	public Team getTeam() {
		return Team.parse(getDetails().getInt32("eventOwnerTeamId").getValue());
	}

	public int getCommittedByPlayerId() {
		return getDetails().getInt32("committedByPlayerId", new BsonInt32(-1)).getValue();
	}

	/**
	 * The "Reason" for the penalty
	 * 
	 * @return
	 */
	public String getDescription() {
		return getDetails().getString("descKey").getValue();
	}

	public int getDuration() {
		return getDetails().getInt32("duration").getValue();
	}

	public String getSeverity() {
		String typeCode = getDetails().getString("typeCode").getValue();
		switch (typeCode) {
		case "MIN":
			return "minor";
		case "MAJ":
			return "major";
		case "MIS":
			return "misconduct";
		default:
			return "";
		}
	}

	/**
	 * Determines if the given event is different from the current event.
	 * 
	 * @param newEvent
	 * @return
	 */
	public boolean isUpdated(PenaltyEvent event) {
		return !getDescription().equals(event.getDescription()) 
				|| !getTeam().equals(event.getTeam())
				|| getCommittedByPlayerId() != event.getCommittedByPlayerId()
				|| getDuration() != event.getDuration() 
				|| !getSeverity().equals(event.getSeverity());
	}
}
