package com.hazeluff.nhl.game;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.nhl.event.GoalEvent;

public class GameTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameTest.class);
	
	@Test
	public void getTopGoalScorersReturnsExpectedValues() {
		LOGGER.info("getTopGoalScorersReturnsExpectedValues");

		assertEquals(Arrays.asList(1), Game.getTopGoalScorers(Arrays.asList(
				mockGoalEvent(1, new Integer[] { 2, 3 }),
				mockGoalEvent(1, new Integer[] { 2 })
		)));

		assertEquals(Arrays.asList(1, 2), Game.getTopGoalScorers(Arrays.asList(
				mockGoalEvent(1, new Integer[] { 2, 3 }),
				mockGoalEvent(2, new Integer[] { 1 }),
				mockGoalEvent(2, new Integer[] { 3 }),
				mockGoalEvent(1, new Integer[] { 3 })
		)));
	}
	
	@Test
	public void getTopPointScorersReturnsExpectedValues() {
		LOGGER.info("getTopPointScorersReturnsExpectedValues");

		assertEquals(Arrays.asList(1), Game.getTopPointScorers(Arrays.asList(
				mockGoalEvent(1, new Integer[] { 2 }),
				mockGoalEvent(1, new Integer[] { 3 }),
				mockGoalEvent(1, new Integer[] { })
		)));

		assertEquals(Arrays.asList(1, 2), Game.getTopPointScorers(Arrays.asList(
				mockGoalEvent(1, new Integer[] { 2, 3 }),
				mockGoalEvent(1, new Integer[] { 2 })
		)));

		assertEquals(Arrays.asList(1, 2, 3), Game.getTopPointScorers(Arrays.asList(
				mockGoalEvent(1, new Integer[] { 2, 3 }),
				mockGoalEvent(1, new Integer[] { }),
				mockGoalEvent(2, new Integer[] { 3 })
				)));
	}
	
	private GoalEvent mockGoalEvent(int scorer, Integer[] assists) {
		GoalEvent event = mock(GoalEvent.class);
		when(event.getScorerId()).thenReturn(scorer);
		when(event.getAssistIds()).thenReturn(Arrays.asList(assists));
		List<Integer> players = new ArrayList<>();
		players.add(scorer);
		players.addAll(Arrays.asList(assists));
		when(event.getPlayerIds()).thenReturn(players);
		return event;
	}
}
