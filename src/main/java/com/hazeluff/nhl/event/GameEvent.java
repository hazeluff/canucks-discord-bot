package com.hazeluff.nhl.event;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.BsonDocument;
import org.bson.BsonValue;

import com.hazeluff.discord.utils.DateUtils;
import com.hazeluff.nhl.Player;
import com.hazeluff.nhl.Team;
import com.hazeluff.nhl.game.EventType;
import com.hazeluff.nhl.game.Period;

public class GameEvent {
	protected BsonDocument rawJson;

	private GameEvent(BsonDocument rawJson) {
		this.rawJson = rawJson;
	}

	protected GameEvent(GameEvent gameEvent) {
		this.rawJson = gameEvent.rawJson;
	}

	public static GameEvent of(BsonDocument rawJson) {
		GameEvent event = new GameEvent(rawJson);
		if (event.getType() == null) {
			return null;
		}
		switch (event.getType()) {
		case GOAL:
			return new GoalEvent(event);
		case PENALTY:
			return new PenaltyEvent(event);
		default:
			return event;
		}
	}

	protected BsonDocument getResultJson() {
		return rawJson.getDocument("result");
	}

	public EventType getType() {
		return EventType.parse(getResultJson().getString("eventTypeId").getValue());
	}

	protected BsonDocument getAboutJson() {
		return rawJson.getDocument("about");
	}

	public int getId() {
		return getAboutJson().getInt32("eventId").getValue();
	}

	public int getIdx() {
		return getAboutJson().getInt32("eventIdx").getValue();
	}

	public ZonedDateTime getDate() {
		return DateUtils.parseNHLDate(getAboutJson().getString("dateTime").getValue());
	}

	public Period getPeriod() {
		return new Period(
				getAboutJson().getInt32("period").getValue(), 
				Period.Type.parse(getAboutJson().getString("periodType").getValue()),
				getAboutJson().getString("ordinalNum").getValue());
	}

	public String getPeriodTime() {
		return getAboutJson().getString("periodTime").getValue();
	}

	public Team getTeam() {
		return Team.parse(rawJson.getDocument("team").getInt32("id").getValue());
	}

	public List<Player> getPlayers() {
		return rawJson.getArray("players").getValues().stream()
				.map(BsonValue::asDocument)
				.map(Player::parse)
				.collect(Collectors.toList());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rawJson == null) ? 0 : rawJson.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GameEvent other = (GameEvent) obj;
		if (rawJson == null) {
			if (other.rawJson != null)
				return false;
		} else if (!rawJson.equals(other.rawJson))
			return false;
		return true;
	}
}
