package com.hazeluff.ahl.game;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.ahl.game.event.Player;
import com.hazeluff.discord.utils.Utils;

public class GameSummaryData {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameSummaryData.class);

	private AtomicReference<BsonDocument> jsonGs;

	private GameSummaryData(BsonDocument jsonGs) {
		this.jsonGs = new AtomicReference<BsonDocument>(jsonGs);
	}
	
	public static GameSummaryData parse(BsonDocument gameSummaryJson) {
		try {
			return new GameSummaryData(gameSummaryJson);
		} catch (Exception e) {
			LOGGER.error("Could not parse json.", e);
			return null;
		}
	}

	public void update(BsonDocument gameSummaryJson) {
		this.jsonGs.set(gameSummaryJson);
	}

	public BsonDocument getJson() {
		return this.jsonGs.get();
	}

	protected BsonDocument getDetails() {
		return getJson().getDocument("details");
	}

	public ZonedDateTime getStartTime() {
		return ZonedDateTime.parse(getDetails().getString("GameDateISO8601").getValue());
	}

	public boolean isStarted() {
		return Utils.numberToBool(getDetails().getString("started").getValue());
	}

	public boolean isFinal() {
		return Utils.numberToBool(getDetails().getString("final").getValue());
	}

	public List<Player> getMVPs() {
		return getJson().getArray("mostValuablePlayers").stream()
				.map(BsonValue::asDocument)
				.map(doc -> doc.getDocument("player"))
				.map(doc -> doc.getDocument("info"))
				.map(Player::parse)
				.collect(Collectors.toList());
	}
	
	public int getHomeGoals() {
		return getJson().getDocument("homeTeam").getDocument("stats").getInt32("goals").getValue();
	}

	public int getAwayGoals() {
		return getJson().getDocument("visitingTeam").getDocument("stats").getInt32("goals").getValue();
	}

	public List<PeriodSummary> getPeriodSummary() {
		return getJson().getArray("periods").stream()
				.map(BsonValue::asDocument)
				.map(PeriodSummary::parse)
				.collect(Collectors.toList());
	}

	public ShootoutDetails getShootoutDetails() {
		return ShootoutDetails.parse(getJson().getDocument("shootoutDetails"));
	}

	@Override
	public String toString() {
		return String.format(
				"GameSummaryData [getStartTime()=%s, isStarted()=%s,"
						+ " isFinal()=%s, getMVPs()=%s, getHomeGoals()=%s, getAwayGoalCount()=%s,"
						+ " getPeriodSummary()=%s, getShootoutDetails()=%s]",
				getStartTime(), isStarted(), isFinal(), getMVPs(), getHomeGoals(),
				getAwayGoals(), getPeriodSummary(), getShootoutDetails());
	}

}
