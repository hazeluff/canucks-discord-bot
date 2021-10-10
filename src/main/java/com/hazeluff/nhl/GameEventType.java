package com.hazeluff.nhl;

import java.util.HashMap;
import java.util.Map;

public enum GameEventType {
	GOAL("GOAL"), PENALTY("PENALTY");

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
		return VALUES_MAP.get(typeId);
	}
}

