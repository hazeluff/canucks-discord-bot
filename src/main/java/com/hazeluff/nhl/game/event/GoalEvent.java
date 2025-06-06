package com.hazeluff.nhl.game.event;

import java.util.ArrayList;
import java.util.List;

import org.bson.BsonDocument;
import org.bson.BsonInt32;

import com.hazeluff.discord.nhl.NHLTeams.Team;

public class GoalEvent extends GameEvent {
	protected GoalEvent(GameEvent gameEvent) {
		super(gameEvent);
	}

	public BsonDocument getDetails() {
		return getJson().getDocument("details");
	}

	public List<Integer> getPlayerIds() {
		List<Integer> playerIds = new ArrayList<>();
		Integer scorerId = getScorerId();
		if (scorerId > 0) {
			playerIds.add(scorerId);
			playerIds.addAll(getAssistIds());
		}
		return playerIds;
	}

	public int getScorerId() {
		return getDetails().getInt32("scoringPlayerId", new BsonInt32(-1)).getValue();
	}

	public List<Integer> getAssistIds() {
		List<Integer> players = new ArrayList<>();
		if (getDetails().containsKey("assist1PlayerId")) {
			players.add(getDetails().getInt32("assist1PlayerId").getValue());
		}
		if (getDetails().containsKey("assist2PlayerId")) {
			players.add(getDetails().getInt32("assist2PlayerId").getValue());
		}
		return players;
	}

	public int getGoalieId() {
		return getDetails().getInt32("goalieInNetId").getValue();
	}

	public Team getTeam() {
		return Team.parse(getDetails().getInt32("eventOwnerTeamId").getValue());
	}

	public boolean isUpdated(GoalEvent event) {
		return !getSituationCode().equals(event.getSituationCode())
				|| getScorerId() != event.getScorerId()
				|| !getAssistIds().equals(event.getAssistIds());
	}

	public boolean isSameTime(GoalEvent event) {
		return getPeriod() == event.getPeriod()
				&& getPeriodTime().equals(event.getPeriodTime());
	}

	@Override
	public String toString() {
		return "GoalEvent [jsonEvent=" + jsonEvent + ", getDetails()=" + getDetails() + ", getPlayerIds()="
				+ getPlayerIds() + ", getScorerId()=" + getScorerId() + ", getAssistIds()=" + getAssistIds()
				+ ", getGoalieId()=" + getGoalieId() + ", getTeam()=" + getTeam() + "]";
	}

}
