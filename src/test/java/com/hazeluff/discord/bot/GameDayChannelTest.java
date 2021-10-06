package com.hazeluff.discord.bot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.nhl.GameTracker;
import com.hazeluff.discord.utils.DateUtils;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.Game;
import com.hazeluff.nhl.GameEvent;
import com.hazeluff.nhl.Team;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DateUtils.class)
public class GameDayChannelTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameDayChannelTest.class);

	private static final Team AWAY_TEAM = Team.VANCOUVER_CANUCKS;
	private static final int AWAY_SCORE = Utils.getRandomInt();
	private static final Team HOME_TEAM = Team.FLORIDA_PANTHERS;
	private static final int HOME_SCORE = Utils.getRandomInt();
	private static final ZoneId TIME_ZONE = ZoneId.of("Canada/Pacific");
	private static final ZonedDateTime DATE = ZonedDateTime.of(2000, 12, 31, 12, 56, 42, 100, ZoneOffset.UTC);
	private List<GameEvent> events;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	NHLBot mockNHLBot;
	@Mock
	GameTracker mockGameTracker;
	@Mock
	Game mockGame;
	@Mock
	Guild mockGuild;
	@Mock
	TextChannel mockChannel;

	Team team = Utils.getRandom(Team.class);

	GameDayChannel gameDayChannel;
	GameDayChannel spyGameDayChannel;

	@Before
	public void before() {
		events = new ArrayList<>();
		when(mockGame.getEvents()).thenReturn(events);

		when(mockGame.getAwayTeam()).thenReturn(AWAY_TEAM);
		when(mockGame.getAwayScore()).thenReturn(AWAY_SCORE);
		when(mockGame.getHomeTeam()).thenReturn(HOME_TEAM);
		when(mockGame.getHomeScore()).thenReturn(HOME_SCORE);
		when(mockGame.getDate()).thenReturn(DATE);

		gameDayChannel = new GameDayChannel(mockNHLBot, mockGameTracker, mockGame, events, mockGuild, mockChannel);
		spyGameDayChannel = spy(gameDayChannel);
	}

	@Test
	public void getShortDateShouldReturnFormattedDate() {
		LOGGER.info("getShortDateShouldReturnFormattedDate");

		String result = gameDayChannel.getShortDate(ZoneId.of("Canada/Pacific"));
		String staticResult = GameDayChannel.getShortDate(mockGame, ZoneId.of("Canada/Pacific"));

		String expected = "00-12-31";
		assertEquals(expected, result);
		assertEquals(expected, staticResult);
	}

	@Test
	public void getNiceDateShouldReturnFormattedDate() {
		LOGGER.info("getNiceDateShouldReturnFormattedDate");

		String result = gameDayChannel.getNiceDate(ZoneId.of("Canada/Pacific"));
		String staticResult = gameDayChannel.getNiceDate(ZoneId.of("Canada/Pacific"));

		String expected = "Sunday 31/Dec/2000";
		assertEquals(expected, result);
		assertEquals(expected, staticResult);
	}

	@Test
	public void getTimeDateShouldReturnFormattedTime() {
		LOGGER.info("getTimeDateShouldReturnFormattedTime");

		String result = gameDayChannel.getTime(ZoneId.of("UTC"));
		String staticResult = GameDayChannel.getTime(mockGame, ZoneId.of("UTC"));

		String expected = "12:56 UTC";
		assertEquals(expected, result);
		assertEquals(expected, staticResult);
	}

	@Test
	public void getTimeDateShouldReturnFormattedAtSpecifiedTimeZone() {
		LOGGER.info("getTimeDateShouldReturnFormattedTime");

		String result = gameDayChannel.getTime(ZoneId.of("Canada/Pacific"));
		String staticResult = GameDayChannel.getTime(mockGame, ZoneId.of("Canada/Pacific"));

		String expected = "4:56 PST";
		assertEquals(expected, result);
		assertEquals(expected, staticResult);
	}

	@Test
	public void getChannelNameShouldReturnFormattedString() {
		LOGGER.info("getChannelNameShouldReturnFormattedString");
		String result = gameDayChannel.getChannelName();
		String staticResult = GameDayChannel.getChannelName(mockGame);

		String expected = "fla-vs-van-00-12-31";
		assertEquals(expected, result);
		assertEquals(expected, staticResult);
	}

	@Test
	public void getDetailsMessageShouldReturnFormattedString() {
		LOGGER.info("getDetailsMessageShouldReturnFormattedString");
		String result = gameDayChannel.getDetailsMessage(TIME_ZONE);
		String staticResult = GameDayChannel.getDetailsMessage(mockGame, TIME_ZONE);

		assertTrue(result.contains(AWAY_TEAM.getFullName()));
		assertTrue(result.contains(HOME_TEAM.getFullName()));
		assertTrue(result.contains(gameDayChannel.getTime(TIME_ZONE)));
		assertTrue(result.contains(gameDayChannel.getNiceDate(TIME_ZONE)));
		assertEquals(result, staticResult);
	}

	@Test
	@PrepareForTest({ DateUtils.class, ZonedDateTime.class, Utils.class })
	public void sendRemindersShouldSendMessages() throws InterruptedException {
		LOGGER.info("sendRemindersShouldSendMessages");
		ZonedDateTime mockCurrentTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime mockGameTime = ZonedDateTime.of(0, 1, 1, 0, 0, 1, 0, ZoneOffset.UTC);
		mockStatic(DateUtils.class, ZonedDateTime.class, Utils.class);
		when(ZonedDateTime.now()).thenReturn(mockCurrentTime);
		when(mockGame.getDate()).thenReturn(mockGameTime);
		when(DateUtils.diffMs(any(ZonedDateTime.class), any(ZonedDateTime.class))).thenReturn(7200000l, 3500000l,
				3400000l, 1700000l, 1600000l, 500000l, 400000l, 0l);
		doNothing().when(spyGameDayChannel).sendMessage(anyString());

		spyGameDayChannel.sendReminders();

		InOrder inOrder = inOrder(spyGameDayChannel);
		inOrder.verify(spyGameDayChannel).sendMessage("60 minutes till puck drop.");
		inOrder.verify(spyGameDayChannel).sendMessage("30 minutes till puck drop.");
		inOrder.verify(spyGameDayChannel).sendMessage("10 minutes till puck drop.");
	}

	@Test
	@PrepareForTest({ DateUtils.class, ZonedDateTime.class, Utils.class })
	public void sendRemindersShouldSkipMessageIfStartedAfterRemindersPassed() throws InterruptedException {
		LOGGER.info("sendRemindersShouldSkipMessageIfStartedAfterRemindersPassed");
		ZonedDateTime mockCurrentTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime mockGameTime = ZonedDateTime.of(0, 1, 1, 0, 0, 1, 0, ZoneOffset.UTC);
		mockStatic(DateUtils.class, ZonedDateTime.class, Utils.class);
		when(ZonedDateTime.now()).thenReturn(mockCurrentTime);
		when(mockGame.getDate()).thenReturn(mockGameTime);
		when(DateUtils.diffMs(any(ZonedDateTime.class), any(ZonedDateTime.class))).thenReturn(1900000l, 1700000l,
				500000l, 0l);
		doNothing().when(spyGameDayChannel).sendMessage(anyString());

		spyGameDayChannel.sendReminders();

		InOrder inOrder = inOrder(spyGameDayChannel);
		inOrder.verify(spyGameDayChannel, never()).sendMessage("60 minutes till puck drop.");
		inOrder.verify(spyGameDayChannel).sendMessage("30 minutes till puck drop.");
		inOrder.verify(spyGameDayChannel).sendMessage("10 minutes till puck drop.");
	}

	@Test
	@PrepareForTest({ DateUtils.class, ZonedDateTime.class, Utils.class })
	public void sendRemindersShouldSleepUntilNearStartOfGame() throws Exception {
		LOGGER.info("sendRemindersShouldSleepUntilNearStartOfGame");

		ZonedDateTime mockCurrentTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime mockGameTime = ZonedDateTime.of(0, 1, 1, 0, 0, 1, 0, ZoneOffset.UTC);
		mockStatic(DateUtils.class, ZonedDateTime.class, Utils.class);
		when(ZonedDateTime.now()).thenReturn(mockCurrentTime);
		when(mockGame.getDate()).thenReturn(mockGameTime);
		when(DateUtils.diffMs(any(ZonedDateTime.class), any(ZonedDateTime.class))).thenReturn(
				GameDayChannel.CLOSE_TO_START_THRESHOLD_MS + 1, GameDayChannel.CLOSE_TO_START_THRESHOLD_MS + 1,
				GameDayChannel.CLOSE_TO_START_THRESHOLD_MS + 1, GameDayChannel.CLOSE_TO_START_THRESHOLD_MS - 1);

		spyGameDayChannel.sendReminders();
		verify(spyGameDayChannel, never()).sendMessage(anyString());
	}

	@Test
	public void isRetryEventFetchShouldReturnBoolean() {
		LOGGER.info("sendRemindersShouldSleepUntilNearStartOfGame");

		List<GameEvent> emptyList = Collections.emptyList();
		List<GameEvent> event1List= Arrays.asList(mock(GameEvent.class));
		List<GameEvent> event2List = Arrays.asList(mock(GameEvent.class), mock(GameEvent.class));

		// returns false when fetchedGameEvents is not empty
		gameDayChannel = new GameDayChannel(null, null, null, null, null, null);
		assertFalse(gameDayChannel.isRetryEventFetch(event1List));

		// returns true if existing list is larger than 1
		gameDayChannel = new GameDayChannel(null, null, null, event2List, null, null);
		assertTrue(gameDayChannel.isRetryEventFetch(emptyList));

		// when list is 1, returns true until iterations reaches threshold
		gameDayChannel = new GameDayChannel(null, null, null, event1List, null, null);
		assertTrue(gameDayChannel.isRetryEventFetch(emptyList));
		assertTrue(gameDayChannel.isRetryEventFetch(emptyList));
		assertTrue(gameDayChannel.isRetryEventFetch(emptyList));
		assertTrue(gameDayChannel.isRetryEventFetch(emptyList));
		assertTrue(gameDayChannel.isRetryEventFetch(emptyList));
		assertFalse(gameDayChannel.isRetryEventFetch(emptyList));
		assertTrue(gameDayChannel.isRetryEventFetch(emptyList));
		assertTrue(gameDayChannel.isRetryEventFetch(emptyList));
		assertTrue(gameDayChannel.isRetryEventFetch(emptyList));
		assertTrue(gameDayChannel.isRetryEventFetch(emptyList));
		assertTrue(gameDayChannel.isRetryEventFetch(emptyList));
		assertFalse(gameDayChannel.isRetryEventFetch(emptyList));
	}
}
