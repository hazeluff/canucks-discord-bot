package com.hazeluff.nhl.game;

import java.util.HashMap;
import java.util.Map;


public enum EventStrength {
	EVEN("EVEN", "Even Strength"), PPG("PPG", "Power Play"), SHORTHANDED("SHG", "Short Handed");

	private final String id;
	private final String value;

	private static final Map<String, EventStrength> VALUES_MAP = new HashMap<>();

	static {
		for (EventStrength s : EventStrength.values()) {
			VALUES_MAP.put(s.id, s);
		}
	}

	private EventStrength(String id, String value) {
		this.id = id;
		this.value = value;
	}

	/**
	 * Gets the Id ("code" in NHL api)
	 * 
	 * @return
	 */
	public String getId() {
		return id;
	}

	public String getValue() {
		return value;
	}

	/**
	 * Parses the id ("code" in NHL api) of the event strength.
	 * 
	 * @param id
	 * @return
	 */
	public static EventStrength parse(String id) {
		EventStrength result = VALUES_MAP.get(id);
		if (result == null) {
			throw new IllegalArgumentException("No value exists for: " + id);
		}
		return result;
	}
}