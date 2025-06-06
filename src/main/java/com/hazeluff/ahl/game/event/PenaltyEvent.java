package com.hazeluff.ahl.game.event;

import com.hazeluff.discord.ahl.AHLTeams.Team;

public class PenaltyEvent extends GameEvent {
	protected PenaltyEvent(GameEvent gameEvent) {
		super(gameEvent);
	}

	public Team getTeam() {
		return Team.parse(getDetails().getDocument("againstTeam").getInt32("id").getValue());
	}

	public int getPeriod() {
		return Integer.valueOf(getDetails().getDocument("period").getString("id").getValue());
	}

	public String getPeriodLongName() {
		return getDetails().getDocument("period").getString("longName").getValue();
	}

	public String getTime() {
		return getDetails().getString("time").getValue();
	}

	public int getPenaltyId() {
		return Integer.valueOf(getDetails().getString("game_penalty_id").getValue());
	}

	public String getDescription() {
		return getDetails().getString("description").getValue();
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

	public boolean isBenchPenalty() {
		return getDetails().getBoolean("isBench").getValue();
	}

	public boolean isPowerPlayAwarded() {
		return getDetails().getBoolean("isPowerPlay").getValue();
	}

	public boolean isUpdated(PenaltyEvent event) {
		return !getDescription().equals(event.getDescription()) 
				|| !getTeam().equals(event.getTeam())
				|| getTakenBy().getId() != event.getTakenBy().getId()
				|| getServedBy().getId() != event.getServedBy().getId()
				|| getDuration() != event.getDuration();
	}

	@Override
	public String toString() {
		return String.format(
				"PenaltyEvent [getTeam()=%s, getPeriod()=%s, getPeriodLongName()=%s, getTime()=%s, getPenaltyId()=%s,"
						+ "getDescription()=%s, getDuration()=%s, getTakenBy()=%s, getServedBy()=%s]",
				getTeam(), getPeriod(), getPeriodLongName(), getTime(), getPenaltyId(), getDescription(), getDuration(),
				getTakenBy(), getServedBy()
		);
	}

}
