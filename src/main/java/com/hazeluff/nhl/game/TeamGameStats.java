package com.hazeluff.nhl.game;

import org.bson.BsonArray;
import org.bson.BsonString;
import org.bson.BsonValue;

public class TeamGameStats {
	private static final String HV = "homeValue";
	private static final String AV = "awayValue";
	
	private int[] sog;
	private double[] foWPct;
	private String[] pp;
	private double[] ppPct;
	private int[] pim;
	private int[] hits;
	private int[] blocked;
	private int[] giveaways;
	private int[] takeaways;

	private TeamGameStats() {

	}

	public static TeamGameStats parse(BsonArray teamGameStatsJson) {
		TeamGameStats stats = new TeamGameStats();
		teamGameStatsJson.stream()
			.map(BsonValue::asDocument)
			.forEach(statJson -> {
					switch (statJson.getString("category", new BsonString("")).getValue()) {
					case "sog":
						stats.sog = new int[] { statJson.getInt32(HV).getValue(),  statJson.getInt32(AV).getValue()};
						break;
					case "faceoffWinningPctg":
						stats.foWPct = new double[] { statJson.getDouble(HV).getValue(), statJson.getDouble(AV).getValue()};
						break;
					case "powerPlay":
						stats.pp = new String[] { statJson.getString(HV).getValue(), statJson.getString(AV).getValue()};
						break;
					case "powerPlayPctg":
						stats.ppPct = new double[] { statJson.getDouble(HV).getValue(), statJson.getDouble(AV).getValue()};
						break;
					case "pim":
						stats.pim = new int[] { statJson.getInt32(HV).getValue(), statJson.getInt32(AV).getValue()};
						break;
					case "hits":
						stats.hits = new int[] { statJson.getInt32(HV).getValue(), statJson.getInt32(AV).getValue() };
						break;
					case "blockedShots":
						stats.blocked = new int[] { statJson.getInt32(HV).getValue(), statJson.getInt32(AV).getValue() };
						break;
					case "giveaways":
						stats.giveaways = new int[] { statJson.getInt32(HV).getValue(),	statJson.getInt32(AV).getValue() };
						break;
					case "takeaways":
						stats.takeaways = new int[] { statJson.getInt32(HV).getValue(), statJson.getInt32(AV).getValue() };
						break;
					default:
						break;
					}
			});
		return stats;
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