package com.hazeluff.discord.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class InterruptableThread extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(InterruptableThread.class);

	protected Logger LOGGER() {
		return LOGGER;
	}

	protected boolean isStop() {
		return isInterrupted();
	}

	protected void sleepFor(long duration) {
		try {
			sleep(duration);
		} catch (InterruptedException e) {
			LOGGER().warn("Sleep interupted");
			interrupt();
		}
	}

	/*
	 * Thread Management
	 */
	@Override
	public void interrupt() {
		super.interrupt();
	}
}
