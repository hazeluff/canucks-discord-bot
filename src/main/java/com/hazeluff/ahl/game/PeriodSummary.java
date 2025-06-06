package com.hazeluff.ahl.game;

import java.util.List;
import java.util.stream.Collectors;

import org.bson.BsonDocument;
import org.bson.BsonValue;

import com.hazeluff.ahl.game.event.GoalDetails;

public class PeriodSummary {
	private final BsonDocument jsonSummary;

	private PeriodSummary(BsonDocument jsonSummary) {
		this.jsonSummary = jsonSummary;
	}

	public static PeriodSummary parse(BsonDocument jsonSummary) {
		return new PeriodSummary(jsonSummary);
	}

	public int getPeriod() {
		return Integer.valueOf(jsonSummary.getDocument("info").getString("id").getValue());
	}

	public String getPeriodLongName() {
		return jsonSummary.getDocument("info").getString("longName").getValue();
	}

	public List<GoalDetails> getGoals() {
		return jsonSummary.getArray("goals").stream()
				.map(BsonValue::asDocument)
				.map(GoalDetails::parse)
				.collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return String.format("PeriodSummary [getPeriod()=%s, getPeriodLongName()=%s, getGoals()=%s]",
				getPeriod(), getPeriodLongName(), getGoals());
	}

}
