package com.hazeluff.discord.utils;

import org.bson.BsonArray;

import com.hazeluff.ahl.AHLGateway;
import com.hazeluff.ahl.game.PlayByPlayData;

public class HazeluffUtils {

	public static void main(String[] argv) {
		BsonArray jsonPbp = AHLGateway.getGamePlayByPlay(1027535);
		System.out.println(PlayByPlayData.parse(jsonPbp));
	}
}
