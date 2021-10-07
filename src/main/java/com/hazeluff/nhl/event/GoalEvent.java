package com.hazeluff.nhl.event;

import com.hazeluff.nhl.GameEventStrength;

public class GoalEvent extends GameEvent {
	protected GoalEvent(GameEvent gameEvent) {
		super(gameEvent);
	}

	public GameEventStrength getStrength() {
		return GameEventStrength.parse(getResultJson().getDocument("strength").getString("code").getValue());
	}
}
