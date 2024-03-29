package com.hazeluff.nhl.game;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum GameType {
	PRESEASON(1), REGULAR(2), PLAYOFF(3);

	private static final Logger LOGGER = LoggerFactory.getLogger(GameType.class);

	private final int value;

	private static Map<Integer, GameType> VALUE_MAP = new HashMap<>();

	static {
		for (GameType type : values()) {
			VALUE_MAP.put(type.value, type);
		}
	}

	private GameType(int value) {
		this.value = value;
	}

	public static GameType parse(Integer value) {
		if (!VALUE_MAP.containsKey(value)) {
			LOGGER.warn("Unknown value: " + value);
		}
		return VALUE_MAP.get(value);
	}

	public boolean isOvertime(int period) {
		switch (this) {
		case PLAYOFF:
			return period > 4;
		default:
			return period == 4;
		}
	}

	public boolean isShootout(int period) {
		switch (this) {
		case PLAYOFF:
			return false;
		default:
			return period == 5;
		}
	}

	// TODO: TEST THIS
	public String getPeriodCode(int period) {
		switch (this) {
		case PRESEASON:
		case REGULAR:
			return _regularCode(period);
		case PLAYOFF:
			return _playoffCode(period);
		default:
			return getOrdinal(period);
		}
	}

	static String _regularCode(int period) {
		switch (period) {
		case 1:
			return "1ST";
		case 2:
			return "2ND";
		case 3:
			return "3RD";
		case 4:
			return "OT";
		case 5:
			return "SO";
		default:
			// Shouldn't happen, but just incase.
			return getOrdinal(period);
		}
	}

	static String _playoffCode(int period) {
		switch (period) {
		case 1:
			return "1ST";
		case 2:
			return "2ND";
		case 3:
			return "3RD";
		default:
			int otPeriod = period - 3;
			return "OT" + otPeriod;
		}
	}

	static String getOrdinal(int value) {
		String[] suffixes = new String[] { "TH", "ST", "ND", "RD", "TH", "TH", "TH", "TH", "TH", "TH" };
		switch (value % 100) {
		case 11:
		case 12:
		case 13:
			return value + "TH";
		default:
			return value + suffixes[value % 10];
		}
	}
}
