package com.hazeluff.nhl.game;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum PeriodType {
	REGULAR("REG"), OVERTIME("OT"), SHOOTOUT("SO");

	private static final Logger LOGGER = LoggerFactory.getLogger(PeriodType.class);

	private final String value;

	private static Map<String, PeriodType> VALUE_MAP = new HashMap<>();

	static {
		for (PeriodType type : values()) {
			VALUE_MAP.put(type.value, type);
		}
	}

	private PeriodType(String value) {
		this.value = value;
	}

	public static PeriodType parse(String value) {
		if (!VALUE_MAP.containsKey(value)) {
			LOGGER.warn("Unknown value: " + value);
		}
		return VALUE_MAP.get(value);
	}
}
