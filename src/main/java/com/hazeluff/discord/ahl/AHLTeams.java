package com.hazeluff.discord.ahl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AHLTeams {

	public static List<Team> getSortedValues() {
		return new ArrayList<>(Team.SORTED_VALUES);
	}

	public enum Team {
		ABBY_NUCKS(440, "Abbotsford", "Abbotsford Canucks", "Canucks", "ABB"),
		BAKERSFIELD(402, "Bakersfield", "Bakersfield Condors", "Condors", "BAK"),
		BELLEVILLE(413, "Belleville", "Belleville Senators", "Senators", "BEL"),
		BRIDGEPORT(317, "Bridgeport", "Bridgeport Islanders", "Islanders", "BRI"),
		CALGARY(444, "Calgary", "Calgary Wranglers", "Wranglers", "CGY"),
		CHARLOTTE(384, "Charlotte", "Charlotte Checkers", "Checkers", "CLT"),
		CHICAGO(330, "Chicago", "Chicago Wolves", "Wolves", "CHI"),
		CLEVELAND(373, "Cleveland", "Cleveland Monsters", "Monsters", "CLE"),
		COACHELLA(445, "Coachella Valley", "Coachella Valley Firebirds", "Firebirds", "CV"),
		COLORADO(419, "Colorado", "Colorado Eagles", "Eagles", "COL"),
		GRAND_RAPID(328, "Grand Rapids", "Grand Rapids Griffins", "Griffins", "GR"),
		HARTFORD(307, "Hartford", "Hartford Wolf Pack", "Wolf Pack", "HFD"),
		HENDERSON(437, "Henderson", "Henderson Silver Knights", "", "HSK"),
		HERSHEY(319, "Hershey", "Hershey Bears", "Bears", "HER"),
		IOWA(389, "Iowa", "Iowa Wild", "Wild", "IA"),
		LAVAL(415, "Laval", "Laval Rocket", "Rocket", "LAV"),
		LE_HIGH(313, "Lehigh Valley", "Lehigh Valley Phantoms", "Phantoms", "LV"),
		MANITOBA(321, "Manitoba", "Manitoba Moose", "Moose", "MB"),
		MILWAUKEE(327, "Milwaukee", "Milwaukee Admirals", "Admirals", "MIL"),
		ONTARIO(403, "Ontario", "Ontario Reign", "Reign", "ONT"),
		PROVIDENCE(309, "Providence", "Providence Bruins", "Bruins", "PRO"),
		ROCHESTER(323, "Rochester", "Rochester Americans", "Americans", "ROC"),
		ROCKFORD(372, "Rockford", "Rockford IceHogs", "IceHogs", "RFD"),
		SAN_DIEGO(404, "San Diego", "San Diego Gulls", "Gulls", "SD"),
		SAN_JOSE(405, "San Jose", "San Jose Barracuda", "Barracuda", "SJ"),
		SPRINGFIELD(411, "Springfield", "Springfield Thunderbirds", "Thunderbirds", "SPR"),
		SYRACUSE(324, "Syracuse", "Syracuse Crunch", "Crunch", "SYR"),
		TEXAS(380, "Texas", "Texas Stars", "Stars", "TEX"),
		TORONTO(335, "Toronto", "Toronto Marlies", "Marlies", "TOR"),
		TUCSON(412, "Tucson", "Tucson Roadrunners", "Roadrunners", "TUC"),
		UTICA(390, "Utica", "Utica Comets", "Comets", "UTC"),
		WBS(316, "Wilkes-Barre/Scranton", "Wilkes-Barre/Scranton Penguins", "Penguins", "WBS"),
		
		ATLANTIC_ALLSTAR(407, "Atlantic Division", "Atlantic Division All-Stars", "Atlantic All-Stars", "ATL"),
		CENTRAL_ALLSTAR(409, "Central Division", "Central Division All-Stars", "Central All-Stars", "CEN"),
		NORTH_ALLSTAR(408, "North Division", "North Division All-Stars", "Pacific All-Stars", "NOR"),
		PACIFIC_ALLSTAR(410, "Pacific Division", "Pacific Division All-Stars", "Pacific All-Stars", "PAC");
		
		private static final Logger LOGGER = LoggerFactory.getLogger(Team.class);
	
		private final int id;
		private final String locationName;
		private final String fullName;
		private final String nickname;
		private final String teamCode;

		private Team(int id, String locationName, String fullName, String nickname, String teamCode) {
			this.id = id;
			this.locationName = locationName;
			this.fullName = fullName;
			this.nickname = nickname;
			this.teamCode = teamCode;
		}

		private static final Map<Integer, Team> VALUES_MAP = new HashMap<>();
		private static final Map<String, Team> CODES_MAP = new HashMap<>();
		private static final List<Team> SORTED_VALUES;
	
		static {
			for (Team t : Team.values()) {
				VALUES_MAP.put(t.id, t);
			}
			for (Team t : Team.values()) {
				CODES_MAP.put(t.teamCode, t);
			}
	
			SORTED_VALUES = new ArrayList<>(new ArrayList<>(EnumSet.allOf(Team.class)));
			SORTED_VALUES.sort((t1, t2) -> t1.fullName.compareTo(t2.fullName));
		}
	
		public int getId() {
			return id;
		}

		public String getLocationName() {
			return locationName;
		}

		public String getFullName() {
			return fullName;
		}

		public String getNickname() {
			return nickname;
		}

		public String getTeamCode() {
			return teamCode;
		}

		public static Team parse(Integer id) {
			if (id == null) {
				return null;
			}
			Team result = VALUES_MAP.get(id);
			if (result == null) {
				LOGGER.warn("No value exists for: " + id);
			}
			return result;
		}
	
		public static boolean isValid(String code) {
			return CODES_MAP.get(code.toUpperCase()) != null;
		}
	
		/**
		 * Parse's a team's code into a Team object
		 * 
		 * @param code
		 *            code of the team
		 * @return
		 */
		public static Team parse(String code) {
			if (code == null || code.isEmpty()) {
				return null;
			}
			Team result = CODES_MAP.get(code.toUpperCase());
			if (result == null) {
				LOGGER.warn("No value exists for: " + code);
			}
			return result;
		}
	}
}