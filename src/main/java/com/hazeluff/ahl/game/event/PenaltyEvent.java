package com.hazeluff.ahl.game.event;

import com.hazeluff.discord.nhl.NHLTeams.Team;

public class PenaltyEvent extends GameEvent {
	protected PenaltyEvent(GameEvent gameEvent) {
		super(gameEvent);
	}

	public Team getTeam() {
		return Team.parse(getDetails().getDocument("againstTeam").getInt32("id").getValue());
	}

	public int getPeriod() {
		return Integer.valueOf(getJson().getDocument("period").getString("id").getValue());
	}

	public String getPeriodLongName() {
		return getJson().getDocument("period").getString("longName").getValue();
	}

	public String getTime() {
		return getDetails().getString("time").getValue();
	}

	public int getPenaltyId() {
		return Integer.valueOf(getDetails().getString("game_penalty_id").getValue());
	}

	public int getDuration() {
		return (int) (float) Float.valueOf(getDetails().getString("minutes").getValue());
	}

	public Player getTakenBy() {
		if (getDetails().containsKey("takenBy")) {
			return Player.parse(getDetails().getDocument("takenBy"));
		}
		return null;
	}

	public Player getServedBy() {
		return Player.parse(getDetails().getDocument("servedBy"));
	}
}
