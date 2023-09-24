package com.hazeluff.nhl.util;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum JSONOperation {
	ADD("add"), REPLACE("replace"), REMOVE("remove");

	private static final Logger LOGGER = LoggerFactory.getLogger(JSONOperation.class);

	private final String value;

	private static Map<String, JSONOperation> VALUE_MAP = new HashMap<>();

	static {
		for (JSONOperation state : values()) {
			VALUE_MAP.put(state.value, state);
		}
	}

	private JSONOperation(String value) {
		this.value = value;
	}

	public static JSONOperation parse(String value) {
		if (!VALUE_MAP.containsKey(value)) {
			LOGGER.warn("Unknown value: " + value);
		}
		return VALUE_MAP.get(value);
	}
}
