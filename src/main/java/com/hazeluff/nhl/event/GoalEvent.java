package com.hazeluff.nhl.event;

import java.util.List;

import com.hazeluff.nhl.Player;
import com.hazeluff.nhl.game.EventStrength;
import com.hazeluff.nhl.game.Period;

public class GoalEvent extends GameEvent {
	protected GoalEvent(GameEvent gameEvent) {
		super(gameEvent);
	}

	@Override
	public List<Player> getPlayers() {
		List<Player> players = super.getPlayers();
		return getPeriod().getType() == Period.Type.SHOOTOUT ? players.subList(0, 1) : players;
	}

	public EventStrength getStrength() {
		return EventStrength.parse(getResultJson().getDocument("strength").getString("code").getValue());
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
