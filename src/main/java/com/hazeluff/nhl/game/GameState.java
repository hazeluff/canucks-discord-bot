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

	private final String code;

	private static Map<String, GameState> CODE_MAP = new HashMap<>();

	static {
		for (GameState state : values()) {
			CODE_MAP.put(state.code, state);
		}
	}

	private GameState(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public static GameState parse(String value) {
		if (!CODE_MAP.containsKey(value)) {
			LOGGER.warn("Unknown value: " + value);
		}
		return CODE_MAP.get(value);
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
		switch (this) {
		case LIVE:
		case CRIT:
			return true;
		default:
			return false;
		}
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
