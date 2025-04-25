package com.hazeluff.discord.utils;

import org.bson.BsonDocument;

import com.hazeluff.ahl.AHLGateway;
import com.hazeluff.ahl.game.GameSummaryData;

public class HazeluffUtils {

	public static void main(String[] argv) {
		BsonDocument jsonSummary = AHLGateway.getGameSummary(1027535);
		System.out.println(GameSummaryData.parse(jsonSummary));
	}
}
