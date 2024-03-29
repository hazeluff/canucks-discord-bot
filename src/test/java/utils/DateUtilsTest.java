package utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.utils.DateUtils;

public class DateUtilsTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(DateUtilsTest.class);
	
	@Test
	public void parseNHLDateShouldReturnDate() {
		LOGGER.info("parseNHLDateShouldReturnDate");
		ZonedDateTime result = DateUtils.parseNHLDate("2000-12-08T15:20:30Z");

		assertEquals(2000, result.getYear());
		assertEquals(12, result.getMonthValue());
		assertEquals(8, result.getDayOfMonth());
		assertEquals(15, result.getHour());
		assertEquals(20, result.getMinute());
		assertEquals(30, result.getSecond());
	}

	@Test
	public void parseNHLDateShouldReturnNullWhenDateIsInvalid() {
		LOGGER.info("parseNHLDateShouldReturnNullWhenDateIsInvalid");
		ZonedDateTime result = DateUtils.parseNHLDate("asdf");

		assertNull(result);
	}

	@Test
	public void isBetweenRangeShouldReturnCorrectAnswer() {
		ZonedDateTime startDate = ZonedDateTime.now();
		ZonedDateTime endDate = startDate.plusDays(5);

		assertFalse(DateUtils.isBetweenRange(startDate.minusDays(1), startDate, endDate));
		assertTrue(DateUtils.isBetweenRange(startDate.plusDays(1), startDate, endDate));
		assertTrue(DateUtils.isBetweenRange(startDate.plusDays(2), startDate, endDate));
		assertTrue(DateUtils.isBetweenRange(startDate.plusDays(3), startDate, endDate));
		assertTrue(DateUtils.isBetweenRange(startDate.plusDays(4), startDate, endDate));
		assertFalse(DateUtils.isBetweenRange(endDate.plusDays(1), startDate, endDate));
	}

}
