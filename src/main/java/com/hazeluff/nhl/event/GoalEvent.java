package com.hazeluff.nhl.event;

import java.util.List;

import com.hazeluff.nhl.GameEventStrength;
import com.hazeluff.nhl.GamePeriod;
import com.hazeluff.nhl.Player;

public class GoalEvent extends GameEvent {
	protected GoalEvent(GameEvent gameEvent) {
		super(gameEvent);
	}

	@Override
	public List<Player> getPlayers() {
		List<Player> players = super.getPlayers();
		return getPeriod().getType() == GamePeriod.Type.SHOOTOUT ? players.subList(0, 1) : players;
	}

	public GameEventStrength getStrength() {
		return GameEventStrength.parse(getResultJson().getDocument("strength").getString("code").getValue());
	}

	@Override
	public String toString() {
		return "GoalEvent [getStrength()=" + getStrength() + ", getResultJson()=" + getResultJson() + ", getType()="
				+ getType() + ", getAboutJson()=" + getAboutJson() + ", getId()=" + getId() + ", getIdx()=" + getIdx()
				+ ", getDate()=" + getDate() + ", getPeriod()=" + getPeriod() + ", getPeriodTime()=" + getPeriodTime()
				+ ", getTeam()=" + getTeam() + ", getPlayers()=" + getPlayers()
				+ "]";
	}

}
