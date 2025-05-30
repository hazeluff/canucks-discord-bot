package com.hazeluff.nhl.game.event;

import java.util.HashMap;
import java.util.Map;

public enum EventType {
	GOAL("goal"), PENALTY("penalty");

	private final String typeId;

	private static final Map<String, EventType> VALUES_MAP = new HashMap<>();

	static {
		for (EventType t : EventType.values()) {
			VALUES_MAP.put(t.typeId, t);
		}
	}

	private EventType(String typeId) {
		this.typeId = typeId;
	}

	public String getValue() {
		return typeId;
	}

	public static EventType parse(String typeId) {
		return VALUES_MAP.get(typeId);
	}
}

