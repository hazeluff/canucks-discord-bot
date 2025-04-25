package com.hazeluff.ahl.game.event;

import com.hazeluff.discord.nhl.NHLTeams.Team;

public class ShootoutEvent extends GameEvent {
	protected ShootoutEvent(GameEvent gameEvent) {
		super(gameEvent);
	}

	public Team getTeam() {
		return Team.parse(getDetails().getDocument("shooterTeam").getInt32("id").getValue());
	}

	public int getPenaltyId() {
		return Integer.valueOf(getDetails().getString("game_penalty_id").getValue());
	}

	public Player getShooter() {
		return Player.parse(getDetails().getDocument("shooter"));
	}

	public boolean isGoal() {
		return getDetails().getBoolean("isGoal").getValue();
	}
}
