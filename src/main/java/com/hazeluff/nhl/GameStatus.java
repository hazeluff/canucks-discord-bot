package com.hazeluff.nhl;

import org.json.JSONObject;

public class GameStatus {
	private final int statusCode;
	private final int codedGameState;
	private final boolean startTimeTBD;
	private final AbstractGameState abstractGameState;
	private final DetailedGameState detailedState;

	private GameStatus(int statusCode, int codedGameState, boolean startTimeTBD, AbstractGameState abstractGameState,
			DetailedGameState detailedState) {
		this.statusCode = statusCode;
		this.codedGameState = codedGameState;
		this.startTimeTBD = startTimeTBD;
		this.abstractGameState = abstractGameState;
		this.detailedState = detailedState;
	}

	public static GameStatus parse(JSONObject json) {
		int statusCode = Integer.parseInt(json.getString("statusCode"));
		int codedGameState = Integer.parseInt(json.getString("codedGameState"));
		boolean startTimeTBD = json.getBoolean("startTimeTBD");
		final AbstractGameState abstractGameState = AbstractGameState.parse(json.getString("abstractGameState"));
		final DetailedGameState detailedState = DetailedGameState.parse(json.getString("detailedState"));
		
		return new GameStatus(statusCode, codedGameState, startTimeTBD, abstractGameState, detailedState);
	}

	public int getStatusCode() {
		return statusCode;
	}

	public int getCodedGameState() {
		return codedGameState;
	}

	public boolean isStartTimeTBD() {
		return startTimeTBD;
	}

	public AbstractGameState getAbstractGameState() {
		return abstractGameState;
	}

	public DetailedGameState getDetailedState() {
		return detailedState;
	}

	public boolean isScheduled() {
		return abstractGameState == AbstractGameState.PREVIEW;
	}

	public boolean isLive() {
		return abstractGameState == AbstractGameState.LIVE;
	}

	public boolean isStarted() {
		return abstractGameState == AbstractGameState.LIVE || abstractGameState == AbstractGameState.FINAL;
	}

	public boolean isFinished() {
		return abstractGameState == AbstractGameState.FINAL;
	}
}
