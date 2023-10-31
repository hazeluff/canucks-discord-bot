package com.hazeluff.nhl.game;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum GameState {
	FUTURE("FUT"), PREGAME("PRE"), LIVE("LIVE"), 
	CRIT("CRIT"), // OT/SO
	FINAL("FINAL"),
	OFF("OFF"); // "Official"?.

	private static final Logger LOGGER = LoggerFactory.getLogger(GameState.class);

	private final String value;

	private static Map<String, GameState> VALUE_MAP = new HashMap<>();

	static {
		for (GameState state : values()) {
			VALUE_MAP.put(state.value, state);
		}
	}

	private GameState(String value) {
		this.value = value;
	}

	public static GameState parse(String value) {
		if (!VALUE_MAP.containsKey(value)) {
			LOGGER.warn("Unknown value: " + value);
		}
		return VALUE_MAP.get(value);
	}

	public boolean isStarted() {
		switch (this) {
		case FUTURE:
		case PREGAME:
			return false;
		default:
			return true;
		}
	}

	public boolean isLive() {
		return LIVE.equals(this);
	}

	public boolean isFinished() {
		switch (this) {
		case FINAL:
		case OFF:
			return true;
		default:
			return false;
		}
	}
}
