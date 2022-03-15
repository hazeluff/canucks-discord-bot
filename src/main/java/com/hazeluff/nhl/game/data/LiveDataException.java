package com.hazeluff.nhl.game.data;

public class LiveDataException extends Throwable {

	private static final long serialVersionUID = 2076725347170125055L;

	public LiveDataException(String message) {
		super(message);
	}

	public LiveDataException(String message, Exception e) {
		super(message, e);
	}

}
