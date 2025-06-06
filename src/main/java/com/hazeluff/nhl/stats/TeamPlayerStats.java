package com.hazeluff.nhl.stats;

import java.util.List;

public class TeamPlayerStats {
	private final List<GoalieStats> goalies;
	private final List<SkaterStats> skaters;

	public TeamPlayerStats(List<GoalieStats> goalies, List<SkaterStats> skaters) {
		this.goalies = goalies;
		this.skaters = skaters;
	}

	public List<GoalieStats> getGoalies() {
		return goalies;
	}

	public List<SkaterStats> getSkaters() {
		return skaters;
	}

	@Override
	public String toString() {
		return "TeamPlayerStats [goalies=" + goalies + ", skaters=" + skaters + "]";
	}
}
