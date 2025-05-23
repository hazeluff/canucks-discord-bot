package com.hazeluff.discord.nhl;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class NHLSeasons {
	public static final Season S24_25 = new Season(
			ZonedDateTime.of(2023, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC),
			ZonedDateTime.of(2024, 7, 31, 0, 0, 0, 0, ZoneOffset.UTC),
			2024,
			2025,
			"24-25");
	public static final Season S23_24 = new Season(
			ZonedDateTime.of(2023, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC),
			ZonedDateTime.of(2024, 7, 31, 0, 0, 0, 0, ZoneOffset.UTC),
			2023,
			2024,
			"23-24");
	public static final Season S22_23 = new Season(
			ZonedDateTime.of(2022, 10, 11, 0, 0, 0, 0, ZoneOffset.UTC),
			ZonedDateTime.of(2023, 7, 31, 0, 0, 0, 0, ZoneOffset.UTC),
			2022,
			2023,
			"22-23");
	public static final Season S21_22 = new Season(
			ZonedDateTime.of(2021, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC),
			ZonedDateTime.of(2021, 7, 31, 0, 0, 0, 0, ZoneOffset.UTC),
			2021,			
			2022,
			"21-22");
	public static final Season S20_21 = new Season(
			ZonedDateTime.of(2021, 1, 12, 0, 0, 0, 0, ZoneOffset.UTC),
			ZonedDateTime.of(2021, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC),
			2020,			
			2021,
			"20-21");

	public static class Season {
		private final ZonedDateTime startDate;
		private final ZonedDateTime endDate;
		private final int startYear;
		private final int endYear;
		private final String abbreviation;

		public Season(ZonedDateTime startDate, ZonedDateTime endDate, int startYear, int endYear, String abbreviation) {
			this.startDate = startDate;
			this.endDate = endDate;
			this.startYear = startYear;
			this.endYear = endYear;
			this.abbreviation = abbreviation;
		}

		public static Season mock(int startYear) {
			return new Season(null, null, startYear, startYear + 1, null);
		}

		public ZonedDateTime getStartDate() {
			return startDate;
		}

		public ZonedDateTime getEndDate() {
			return endDate;
		}

		public int getStartYear() {
			return startYear;
		}

		public int getEndYear() {
			return endYear;
		}

		public String getAbbreviation() {
			return abbreviation;
		}
	}

}
