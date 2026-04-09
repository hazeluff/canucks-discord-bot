package com.hazeluff.discord.bot;

public class SchedulerException extends RuntimeException {
	private static final long serialVersionUID = -7091697683452291095L;

	public SchedulerException(RuntimeException e) {
		super(e);
	}

	public SchedulerException(String string) {
		super(string);
	}

}
