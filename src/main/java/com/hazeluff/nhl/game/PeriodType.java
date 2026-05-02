package com.hazeluff.nhl.game;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum PeriodType {
	REGULAR("REG", "Regulation"), OVERTIME("OT", "Overtime"), SHOOTOUT("SO", "Shootout");

	private static final Logger LOGGER = LoggerFactory.getLogger(PeriodType.class);

	private final String code;
	private final String niceName;

	private static Map<String, PeriodType> CODE_MAP = new HashMap<>();

	static {
		for (PeriodType type : values()) {
			CODE_MAP.put(type.code, type);
		}
	}

	private PeriodType(String code, String niceName) {
		this.code = code;
		this.niceName = niceName;
	}

	public static PeriodType parse(String value) {
		if (!CODE_MAP.containsKey(value)) {
			LOGGER.warn("Unknown value: " + value);
		}
		return CODE_MAP.get(value);
	}

	public String getCode() {
		return code;
	}

	public String getNiceName() {
		return niceName;
	}
}
