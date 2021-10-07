package com.hazeluff.nhl;

import java.util.HashMap;
import java.util.Map;

public enum GameEventType {
	GOAL("GOAL");

	private final String typeId;

	private static final Map<String, GameEventType> VALUES_MAP = new HashMap<>();

	static {
		for (GameEventType t : GameEventType.values()) {
			VALUES_MAP.put(t.typeId, t);
		}
	}

	private GameEventType(String typeId) {
		this.typeId = typeId;
	}

	public String getValue() {
		return typeId;
	}

	public static GameEventType parse(String typeId) {
		GameEventType result = VALUES_MAP.get(typeId);
		if (result == null) {
			throw new IllegalArgumentException("No value exists for: " + typeId);
		}
		return result;
	}
}

