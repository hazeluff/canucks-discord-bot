package com.hazeluff.ahl.game.event;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.BsonDocument;
import org.bson.BsonValue;

import com.hazeluff.discord.ahl.AHLTeams.Team;
import com.hazeluff.discord.utils.Utils;

public class GoalDetails {

	private final BsonDocument jsonDetails;

	private GoalDetails(BsonDocument jsonDetails) {
		this.jsonDetails = jsonDetails;
	}

	public static GoalDetails parse(BsonDocument jsonDetails) {
		return new GoalDetails(jsonDetails);
	}

	public int getGoalId() {
		return Integer.valueOf(jsonDetails.getString("game_goal_id").getValue());
	}

	public Team getTeam() {
		return Team.parse(jsonDetails.getDocument("team").getInt32("id").getValue());
	}

	public int getPeriod() {
		return Integer.valueOf(jsonDetails.getDocument("period").getString("id").getValue());
	}

	public String getTime() {
		return jsonDetails.getString("time").getValue();
	}

	public String getPeriodLongName() {
		return jsonDetails.getDocument("period").getString("longName").getValue();
	}

	public Player getScorer() {
		return Player.parse(jsonDetails.getDocument("scoredBy"));
	}

	public List<Player> getAssists() {
		return jsonDetails.getArray("assists").stream().map(BsonValue::asDocument).map(Player::parse)
				.collect(Collectors.toList());
	}

	public List<Player> getPlayers() {
		List<Player> players = Arrays.asList(getScorer());
		players.addAll(getAssists());
		return players;
	}

	public List<Integer> getPlayerIds() {
		return getPlayers().stream()
				.map(Player::getId)
				.collect(Collectors.toList());
	}

	protected BsonDocument getProperties() {
		return jsonDetails.getDocument("properties");
	}

	public boolean isEmptyNet() {
		return Utils.numberToBool(getProperties().getString("isEmptyNet").getValue());
	}

	public boolean isGameWinningGoal() {
		return Utils.numberToBool(getProperties().getString("isGameWinningGoal").getValue());
	}

	public boolean isInsuranceGoal() {
		return Utils.numberToBool(getProperties().getString("isInsuranceGoal").getValue());
	}

	public boolean isPenaltyShot() {
		return Utils.numberToBool(getProperties().getString("isPenaltyShot").getValue());
	}

	public boolean isPowerPlay() {
		return Utils.numberToBool(getProperties().getString("isPowerPlay").getValue());
	}

	public boolean isShortHanded() {
		return Utils.numberToBool(getProperties().getString("isShortHanded").getValue());
	}

	@Override
	public String toString() {
		return String.format(
				"GoalDetails [getGoalId()=%s, getTeam()=%s, getPeriod()=%s, getTime()=%s, getPeriodLongName()=%s,"
						+ " getScorer()=%s, getAssists()=%s, getProperties()=%s, isEmptyNet()=%s,"
						+ " isGameWinningGoal()=%s, isInsuranceGoal()=%s, isPenaltyShot()=%s, isPowerPlay()=%s,"
						+ " isShortHanded()=%s]",
				getGoalId(), getTeam(), getPeriod(), getTime(), getPeriodLongName(), getScorer(), getAssists(),
				getProperties(), isEmptyNet(), isGameWinningGoal(), isInsuranceGoal(), isPenaltyShot(), isPowerPlay(),
				isShortHanded());
	}
}
