package com.hazeluff.ahl.game;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.bson.BsonArray;
import org.bson.BsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.ahl.game.event.GameEvent;


public class PlayByPlayData {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlayByPlayData.class);

	private AtomicReference<BsonArray> jsonPbp;

	private PlayByPlayData(BsonArray jsonPbp) {
		this.jsonPbp = new AtomicReference<BsonArray>(jsonPbp);
	}
	
	public static PlayByPlayData parse(BsonArray playByPlayJson) {
		try {
			return new PlayByPlayData(playByPlayJson);
		} catch (Exception e) {
			LOGGER.error("Could not parse json.", e);
			return null;
		}
	}

	public void update(BsonArray playByPlayJson) {
		this.jsonPbp.set(playByPlayJson);
	}

	public List<GameEvent> getPlays() {
		return getJson().getValues().stream()
			.map(BsonValue::asDocument)
			.map(GameEvent::parse)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	public BsonArray getJson() {
		return this.jsonPbp.get();
	}

	@Override
	public String toString() {
		return String.format("PlayByPlayData [getPlays()=%s]", getPlays());
	}
}
