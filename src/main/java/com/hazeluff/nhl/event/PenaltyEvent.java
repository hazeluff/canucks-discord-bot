package com.hazeluff.nhl.event;

import java.util.List;

import com.hazeluff.nhl.GamePeriod;
import com.hazeluff.nhl.Player;

public class PenaltyEvent extends GameEvent {
	protected PenaltyEvent(GameEvent gameEvent) {
		super(gameEvent);
	}

	@Override
	public List<Player> getPlayers() {
		List<Player> players = super.getPlayers();
		return getPeriod().getType() == GamePeriod.Type.SHOOTOUT ? players.subList(0, 1) : players;
	}

	public String getDescription() {
		return getResultJson().getString("description").getValue();
	}

	public int getMinutes() {
		return getResultJson().getInt32("penaltyMinutes").getValue();
	}

	public String getSeverity() {
		return getResultJson().getString("penaltySeverity").getValue();
	}

	public String getSecondaryType() {
		return getResultJson().getString("secondaryType").getValue();
	}
}
