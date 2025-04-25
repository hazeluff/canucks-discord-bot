package com.hazeluff.discord.bot.gdc.custom.game;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.nhl.NHLTeams.Team;
import com.hazeluff.nhl.game.Game;

public class CustomGameMessageTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(CustomGameMessageTest.class);
	
	private static final Team TEAM_W = Team.VANCOUVER_CANUCKS;
	private static final Team TEAM_L = Team.CHICAGO_BLACKHAWKS;

	@Test
	public void winReturnsExpectedValues() {
		LOGGER.info("winReturnsExpectedValues");

		Predicate<Game> testConditions = CustomGameMessage.Collection.buildWinConditions(TEAM_W);
		assertTrue(testConditions.test(mockGame(TEAM_W, TEAM_L)));
		assertFalse(testConditions.test(mockGame(TEAM_L, TEAM_W)));
	}

	@Test
	public void shutoutReturnsExpectedValues() {
		LOGGER.info("shutoutReturnsExpectedValues");

		Predicate<Game> testConditions = CustomGameMessage.Collection.buildShutoutConditions(TEAM_W);
		assertTrue(testConditions.test(mockGame(TEAM_W, TEAM_L, 1, 0, TEAM_W)));
		assertFalse(testConditions.test(mockGame(TEAM_W, TEAM_L, 0, 1, TEAM_W)));
		assertFalse(testConditions.test(mockGame(TEAM_W, TEAM_L, 2, 1, TEAM_W)));

		assertFalse(testConditions.test(mockGame(TEAM_L, TEAM_W, 1, 0, TEAM_L)));
		assertFalse(testConditions.test(mockGame(TEAM_L, TEAM_W, 0, 1, TEAM_L)));
		assertFalse(testConditions.test(mockGame(TEAM_L, TEAM_W, 2, 1, TEAM_L)));
	}

	@Test
	public void mostGoalsOrPointsReturnsExpectedValues() {
		LOGGER.info("mostGoalsOrPointsReturnsExpectedValues");

		Predicate<Game> testConditions = CustomGameMessage.Collection.buildMostGoalsOrPointsCondition(TEAM_W, 1);
		assertTrue(testConditions.test(mockGame(TEAM_W, Arrays.asList(1), Arrays.asList(1))));
		assertTrue(testConditions.test(mockGame(TEAM_W, Arrays.asList(1), Arrays.asList(2))));
		assertTrue(testConditions.test(mockGame(TEAM_W, Arrays.asList(2), Arrays.asList(1))));
		assertTrue(testConditions.test(mockGame(TEAM_W, Arrays.asList(1, 2), Arrays.asList(1))));
		assertTrue(testConditions.test(mockGame(TEAM_W, Arrays.asList(1, 2), Arrays.asList(2))));
		assertTrue(testConditions.test(mockGame(TEAM_W, Arrays.asList(1), Arrays.asList(1, 2))));
		assertTrue(testConditions.test(mockGame(TEAM_W, Arrays.asList(2), Arrays.asList(1, 2))));

		assertFalse(testConditions.test(mockGame(TEAM_W, Arrays.asList(2), Arrays.asList(3))));

		assertFalse(testConditions.test(mockGame(TEAM_L, Arrays.asList(1), Arrays.asList(1))));
		assertFalse(testConditions.test(mockGame(TEAM_L, Arrays.asList(1), Arrays.asList(2))));
		assertFalse(testConditions.test(mockGame(TEAM_L, Arrays.asList(2), Arrays.asList(1))));
		assertFalse(testConditions.test(mockGame(TEAM_L, Arrays.asList(1, 2), Arrays.asList(1))));
		assertFalse(testConditions.test(mockGame(TEAM_L, Arrays.asList(1, 2), Arrays.asList(2))));
		assertFalse(testConditions.test(mockGame(TEAM_L, Arrays.asList(1), Arrays.asList(1, 2))));
		assertFalse(testConditions.test(mockGame(TEAM_L, Arrays.asList(2), Arrays.asList(1, 2))));
	}

	private Game mockGame(Team winningTeam, Team losingTeam) {
		Game game = mock(Game.class);
		when(game.getWinningTeam()).thenReturn(winningTeam);
		return game;
	}

	private Game mockGame(Team homeTeam, Team awayTeam, int homeScore, int awayScore, Team winningTeam) {
		Game game = mock(Game.class);
		when(game.getHomeTeam()).thenReturn(homeTeam);
		when(game.getAwayTeam()).thenReturn(awayTeam);
		when(game.getHomeScore()).thenReturn(homeScore);
		when(game.getAwayScore()).thenReturn(awayScore);
		when(game.getWinningTeam()).thenReturn(winningTeam);
		return game;
	}

	private Game mockGame(Team winningTeam, List<Integer> topGoalScorer, List<Integer> topPointsScorer) {
		Game game = mock(Game.class);
		when(game.getWinningTeam()).thenReturn(winningTeam);
		when(game.getTopGoalScorers()).thenReturn(topGoalScorer);
		when(game.getTopPointScorers()).thenReturn(topPointsScorer);
		return game;
	}
}
