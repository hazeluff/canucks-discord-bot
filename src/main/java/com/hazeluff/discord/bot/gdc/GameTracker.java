package com.hazeluff.discord.bot.gdc;

public interface GameTracker {

	public void updateGame();
	public boolean isFinished();	
	public boolean isGameFinished();
	public boolean isGameStarted();
}
