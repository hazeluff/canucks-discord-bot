package com.hazeluff.discord.utils;

public class HttpException extends Exception {
	private static final long serialVersionUID = 1336567484487343944L;

	public HttpException(String message) {
		super(message);
	}

	public HttpException(Throwable t) {
		super(t);
	}

	public HttpException(String message, Throwable t) {
		super(message, t);
	}
}
