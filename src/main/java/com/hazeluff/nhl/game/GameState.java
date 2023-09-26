package com.hazeluff.nhl.game;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum GameState {
	FUTURE("FUT"), PREGAME("PRE"), LIVE("LIVE"), CRIT("CRIT"), FINAL("FINAL"),
	 OFF("OFF"); // This should be filtered out. ("Outside of current season")

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

	public boolean isFinal() {
		return FINAL.equals(this);
	}
}
