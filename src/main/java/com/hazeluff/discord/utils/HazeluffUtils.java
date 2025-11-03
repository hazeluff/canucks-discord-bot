package com.hazeluff.discord.utils;

import com.hazeluff.discord.Config;

public class HazeluffUtils {

	public static void main(String[] argv) {
		System.out.println("van-vs-lak-25-01-02".matches(Config.NHL_CHANNEL_REGEX));
		System.out.println("van-vs-lak-26-01-02".matches(Config.NHL_CHANNEL_REGEX));
	}
}
