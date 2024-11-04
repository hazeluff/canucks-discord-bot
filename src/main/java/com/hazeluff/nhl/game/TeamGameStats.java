package com.hazeluff.nhl.game;

import org.bson.BsonArray;
import org.bson.BsonString;
import org.bson.BsonValue;

public class TeamGameStats {
	private static final String HV = "homeValue";
	private static final String AV = "awayValue";
	
	private final int[] sog;
	private final double[] foWPct;
	private final String[] pp;
	private final double[] ppPct;
	private final int[] pim;
	private final int[] hits;
	private final int[] blocked;
	private final int[] giveaways;
	private final int[] takeaways;

	private TeamGameStats() {
		this.sog = new int[] { 0, 0 };
		this.foWPct = new double[] { 0f, 0f };
		this.pp = new String[] { "0/0", "0/0" };
		this.ppPct = new double[] { 0f, 0f };
		this.pim = new int[] { 0, 0 };
		this.hits = new int[] { 0, 0 };
		this.blocked = new int[] { 0, 0 };
		this.giveaways = new int[] { 0, 0 };
		this.takeaways = new int[] { 0, 0 };
	}

	public static TeamGameStats parse(BsonArray teamGameStatsJson) {
		TeamGameStats stats = new TeamGameStats();
		stats.update(teamGameStatsJson);
		return stats;
	}

	public void update(BsonArray teamGameStatsJson) {
		teamGameStatsJson.stream().map(BsonValue::asDocument).forEach(statJson -> {
			switch (statJson.getString("category", new BsonString("")).getValue()) {
			case "sog":
				this.sog[0] = statJson.getInt32(HV).getValue();
				this.sog[1] = statJson.getInt32(AV).getValue();
				break;
			case "faceoffWinningPctg":
				this.foWPct[0] = statJson.getDouble(HV).getValue();
				this.foWPct[1] = statJson.getDouble(AV).getValue();
				break;
			case "powerPlay":
				this.pp[0] = statJson.getString(HV).getValue();
				this.pp[1] = statJson.getString(AV).getValue();
				break;
			case "powerPlayPctg":
				this.ppPct[0] = statJson.getDouble(HV).getValue();
				this.ppPct[1] = statJson.getDouble(AV).getValue();
				break;
			case "pim":
				this.pim[0] = statJson.getInt32(HV).getValue();
				this.pim[1] = statJson.getInt32(AV).getValue();
				break;
			case "hits":
				this.hits[0] = statJson.getInt32(HV).getValue();
				this.hits[1] = statJson.getInt32(AV).getValue();
				break;
			case "blockedShots":
				this.blocked[0] = statJson.getInt32(HV).getValue();
				this.blocked[1] = statJson.getInt32(AV).getValue();
				break;
			case "giveaways":
				this.giveaways[0] = statJson.getInt32(HV).getValue();
				this.giveaways[1] = statJson.getInt32(AV).getValue();
				break;
			case "takeaways":
				this.takeaways[0] = statJson.getInt32(HV).getValue();
				this.takeaways[1] = statJson.getInt32(AV).getValue();
				break;
			default:
				break;
			}
		});
	}

	public int[] getSog() {
		return sog;
	}

	public double[] getFoWPct() {
		return foWPct;
	}

	public String[] getPp() {
		return pp;
	}

	public double[] getPpPct() {
		return ppPct;
	}

	public int[] getPim() {
		return pim;
	}

	public int[] getHits() {
		return hits;
	}

	public int[] getBlocked() {
		return blocked;
	}

	public int[] getGiveaways() {
		return giveaways;
	}

	public int[] getTakeaways() {
		return takeaways;
	}

}