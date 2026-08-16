package com.hazeluff.discord.bot.database.preferences;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum PlayoffMode {

	OFF(0), SING_CHNL_NO_THRD(1), SING_CHNL_W_THRD(2);

	private static final Logger LOGGER = LoggerFactory.getLogger(PlayoffMode.class);

	private final int id;

	private static Map<Integer, PlayoffMode> MODE_MAP = new HashMap<>();

	static {
		for (PlayoffMode type : values()) {
			MODE_MAP.put(type.id, type);
		}
	}

	public static PlayoffMode parse(int value) {
		if (!MODE_MAP.containsKey(value)) {
			LOGGER.warn("Unknown value: " + value);
		}
		return MODE_MAP.get(value);
	}

	PlayoffMode(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public boolean isEnabled() {
		return this != OFF;
	}

	public boolean isUseThreads() {
		return this == SING_CHNL_W_THRD;
	}

}
