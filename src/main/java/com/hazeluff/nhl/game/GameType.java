package com.hazeluff.nhl.game;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.utils.Utils;

public enum GameType {
	PRESEASON(1), REGULAR(2), PLAYOFF(3), FOUR_NATIONS(19), FOUR_NATIONS_FINAL(20);

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

	public boolean isFourNations() {
		switch (this) {
		case FOUR_NATIONS:
		case FOUR_NATIONS_FINAL:
			return true;
		default:
			return false;
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
		case FOUR_NATIONS:
			return _regularCode(period);
		case FOUR_NATIONS_FINAL:
			return _playoffCode(period);
		default:
			return Utils.getOrdinal(period);
		}
	}

	static String _regularCode(int period) {
		switch (period) {
		case 1:
			return "1st";
		case 2:
			return "2nd";
		case 3:
			return "3rd";
		case 4:
			return "OT";
		case 5:
			return "SO";
		default:
			// Shouldn't happen, but just incase.
			return Utils.getOrdinal(period);
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
}
