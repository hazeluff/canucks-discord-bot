package com.hazeluff.discord.utils;

public class HazeluffUtils {

	public static void main(String[] argv) {
		try {
			System.out.println("Start");
			throw new AbstractMethodError("test");
		} catch (RuntimeException e) {
			System.out.println("Caught");
		}
		System.out.println("After");
	}
}
