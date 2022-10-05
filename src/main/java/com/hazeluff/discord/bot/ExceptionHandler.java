package com.hazeluff.discord.bot;

import java.lang.Thread.UncaughtExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionHandler implements UncaughtExceptionHandler {
	private final Logger LOGGER;

	public ExceptionHandler() {
		LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		LOGGER.error("UnhandledException in [" + t.getName() + "]", e);
	}

}
