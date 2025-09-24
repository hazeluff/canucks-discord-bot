package com.hazeluff.discord.nhl;

public class NHLSeasons {
	public static final Season S25_26 = new Season(2025, "25-26");
	public static final Season S24_25 = new Season(2024, "24-25");
	public static final Season S23_24 = new Season(2023, "23-24");
	public static final Season S22_23 = new Season(2022, "22-23");
	public static final Season S21_22 = new Season(2021, "21-22");
	public static final Season S20_21 = new Season(2020, "20-21");

	public static class Season {
		private final int startYear;
		private final int endYear;
		private final String abbreviation;

		public Season(int startYear, String abbreviation) {
			this.startYear = startYear;
			this.endYear = startYear + 1;
			this.abbreviation = abbreviation;
		}

		public static Season mock(int startYear) {
			return new Season(startYear, null);
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
