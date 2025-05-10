package com.hazeluff.ahl.game.event;

import org.bson.BsonDocument;

public class GoalEvent extends GameEvent {
	public GoalEvent(BsonDocument jsonEvent) {
		super(jsonEvent);
	}

	protected GoalEvent(GameEvent gameEvent) {
		super(gameEvent);
	}
	
	public GoalDetails getGoalDetails() {
		return GoalDetails.parse(getDetails());
	}

	public int getGoalId() {
		return getGoalDetails().getGoalId();
	}

	public boolean isSameTime(GoalEvent event) {
		return getGoalDetails().getPeriod() == event.getGoalDetails().getPeriod()
				&& getGoalDetails().getTime().equals(event.getGoalDetails().getTime());
	}

	public boolean isUpdated(GoalEvent newEvent) {
		GoalDetails cachedDetails = getGoalDetails();
		GoalDetails newDetails = newEvent.getGoalDetails();

		boolean isSpecialDetailDiff = cachedDetails.isEmptyNet() != newDetails.isEmptyNet()
				|| cachedDetails.isGameWinningGoal() != newDetails.isGameWinningGoal()
				|| cachedDetails.isPenaltyShot() != newDetails.isPenaltyShot()
				|| cachedDetails.isPowerPlay() != newDetails.isPowerPlay()
				|| cachedDetails.isShortHanded() != newDetails.isShortHanded();

		boolean isPlayersDiff = !cachedDetails.getPlayerIds().equals(newDetails.getPlayerIds());

		return isSpecialDetailDiff || isPlayersDiff;
	}

	@Override
	public String toString() {
		return String.format("GoalEvent [getGoalDetails()=%s, getType()=%s]", getGoalDetails(), getType());
	}
}
