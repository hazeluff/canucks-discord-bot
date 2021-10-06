package com.hazeluff.nhl;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum DetailedGameState {

	SCHEDULED("Scheduled"), 
	POSTPONED("Postponed"), 
	PRE_GAME("Pre-Game"), 
	IN_PROGRESS("In Progress"), 
	FINAL("Final");

	private static final Logger LOGGER = LoggerFactory.getLogger(DetailedGameState.class);

	private final String value;

	private static Map<String, DetailedGameState> VALUE_MAP = new HashMap<>();

	static {
		for (DetailedGameState state : values()) {
			VALUE_MAP.put(state.value, state);
		}
	}

	private DetailedGameState(String value) {
		this.value = value;
	}

	public static DetailedGameState parse(String value) {
		if (!VALUE_MAP.containsKey(value)) {
			LOGGER.warn("Unknown value: " + value);
		}
		return VALUE_MAP.get(value);
	}

	@Override
	public String toString() {
		return value;
	}
}
