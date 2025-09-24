package com.hazeluff.discord.ahl;

public class AHLSeasons {
	public static final Season S25_26 = new Season(
			89, 90, 91, 92,
			2025, 2026, "25-26");
	public static final Season S24_25 = new Season(
			85, 86, 87, 88,
			2024, 2025, "24-25");

	public static class Season {
		private final int preSeasonId;
		private final int seasonId;
		private final int seasonAllstarId;
		private final int seasonPlayoffId;
		private final int startYear;
		private final int endYear;
		private final String abbreviation;

		public Season(int preSeasonId, int seasonId, int seasonAllstarId, int seasonPlayoffId, int startYear,
				int endYear, String abbreviation) {
			this.preSeasonId = preSeasonId;
			this.seasonId = seasonId;
			this.seasonAllstarId = seasonAllstarId;
			this.seasonPlayoffId = seasonPlayoffId;
			this.startYear = startYear;
			this.endYear = endYear;
			this.abbreviation = abbreviation;
		}

		public int getPreSeasonId() {
			return preSeasonId;
		}

		public int getSeasonId() {
			return seasonId;
		}

		public int getSeasonAllstarId() {
			return seasonAllstarId;
		}

		public int getSeasonPlayoffId() {
			return seasonPlayoffId;
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
