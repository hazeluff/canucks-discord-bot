package com.hazeluff.ahl.game.event;

import com.hazeluff.discord.ahl.AHLTeams.Team;

public class ShootoutEvent extends GameEvent {
	protected ShootoutEvent(GameEvent gameEvent) {
		super(gameEvent);
	}

	public Team getTeam() {
		return Team.parse(getDetails().getDocument("shooterTeam").getInt32("id").getValue());
	}

	public Player getShooter() {
		return Player.parse(getDetails().getDocument("shooter"));
	}

	public boolean isGoal() {
		return getDetails().getBoolean("isGoal").getValue();
	}

	@Override
	public String toString() {
		return String.format("ShootoutEvent [getTeam()=%s, getShooter()=%s, isGoal()=%s]", 
				getTeam(), getShooter(), isGoal());
	}
	
	
}
