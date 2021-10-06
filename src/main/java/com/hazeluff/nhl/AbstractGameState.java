package com.hazeluff.nhl;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum AbstractGameState {

	PREVIEW("Preview"), LIVE("Live"), FINAL("Final");

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGameState.class);

	private final String value;

	private static Map<String, AbstractGameState> VALUE_MAP = new HashMap<>();

	static {
		for (AbstractGameState state : values()) {
			VALUE_MAP.put(state.value, state);
		}
	}

	private AbstractGameState(String value) {
		this.value = value;
	}

	public static AbstractGameState parse(String value) {
		if(!VALUE_MAP.containsKey(value)) {
			LOGGER.warn("Unknown value: " + value);
		}
		return VALUE_MAP.get(value);
	}
}
