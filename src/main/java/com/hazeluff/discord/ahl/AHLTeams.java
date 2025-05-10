package com.hazeluff.discord.ahl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import discord4j.rest.util.Color;

public class AHLTeams {

	public static List<Team> getSortedValues() {
		return new ArrayList<>(Team.SORTED_VALUES);
	}

	public enum Team {
		ABBY_NUCKS(440, "Abbotsford", "Abbotsford Canucks", "Canucks", "ABB", Color.of(0x047835)),
		BAKERSFIELD(402, "Bakersfield", "Bakersfield Condors", "Condors", "BAK", Color.of(0xDF4E10)),
		BELLEVILLE(413, "Belleville", "Belleville Senators", "Senators", "BEL", Color.of(0xE3173E)),
		BRIDGEPORT(317, "Bridgeport", "Bridgeport Islanders", "Islanders", "BRI", Color.of(0xF26924)),
		CALGARY(444, "Calgary", "Calgary Wranglers", "Wranglers", "CGY", Color.of(0xC2273D)),
		CHARLOTTE(384, "Charlotte", "Charlotte Checkers", "Checkers", "CLT", Color.of(0xE51A38)),
		CHICAGO(330, "Chicago", "Chicago Wolves", "Wolves", "CHI", Color.of(0x939283)),
		CLEVELAND(373, "Cleveland", "Cleveland Monsters", "Monsters", "CLE", Color.of(0x005695)),
		COACHELLA(445, "Coachella Valley", "Coachella Valley Firebirds", "Firebirds", "CV", Color.of(0xC8102E)),
		COLORADO(419, "Colorado", "Colorado Eagles", "Eagles", "COL", Color.of(0xFFD457)),
		GRAND_RAPID(328, "Grand Rapids", "Grand Rapids Griffins", "Griffins", "GR", Color.of(0x866C3F)),
		HARTFORD(307, "Hartford", "Hartford Wolf Pack", "Wolf Pack", "HFD", Color.of(0x00548E)),
		HENDERSON(437, "Henderson", "Henderson Silver Knights", "", "HSK", Color.of(0xC2C4C6)),
		HERSHEY(319, "Hershey", "Hershey Bears", "Bears", "HER", Color.of(0x7E543A)),
		IOWA(389, "Iowa", "Iowa Wild", "Wild", "IA", Color.of(0x144733)),
		LAVAL(415, "Laval", "Laval Rocket", "Rocket", "LAV", Color.of(0x001E61)),
		LE_HIGH(313, "Lehigh Valley", "Lehigh Valley Phantoms", "Phantoms", "LV", Color.of(0xF58220)),
		MANITOBA(321, "Manitoba", "Manitoba Moose", "Moose", "MB", Color.of(0x041E41)),
		MILWAUKEE(327, "Milwaukee", "Milwaukee Admirals", "Admirals", "MIL", Color.of(0x83C2EC)),
		ONTARIO(403, "Ontario", "Ontario Reign", "Reign", "ONT", Color.of(0xA4A9AD)),
		PROVIDENCE(309, "Providence", "Providence Bruins", "Bruins", "PRO", Color.of(0xFBB337)),
		ROCHESTER(323, "Rochester", "Rochester Americans", "Americans", "ROC", Color.of(0x393A87)),
		ROCKFORD(372, "Rockford", "Rockford IceHogs", "IceHogs", "RFD", Color.of(0xDB1931)),
		SAN_DIEGO(404, "San Diego", "San Diego Gulls", "Gulls", "SD", Color.of(0xFF4C00)),
		SAN_JOSE(405, "San Jose", "San Jose Barracuda", "Barracuda", "SJ", Color.of(0x266B73)),
		SPRINGFIELD(411, "Springfield", "Springfield Thunderbirds", "Thunderbirds", "SPR", Color.of(0xFFC425)),
		SYRACUSE(324, "Syracuse", "Syracuse Crunch", "Crunch", "SYR", Color.of(0x1D427C)),
		TEXAS(380, "Texas", "Texas Stars", "Stars", "TEX", Color.of(0x1B6031)),
		TORONTO(335, "Toronto", "Toronto Marlies", "Marlies", "TOR", Color.of(0x003E7E)),
		TUCSON(412, "Tucson", "Tucson Roadrunners", "Roadrunners", "TUC", Color.of(0x8E0A26)),
		UTICA(390, "Utica", "Utica Comets", "Comets", "UTC", Color.of(0xCF2031)),
		WBS(316, "Wilkes-Barre/Scranton", "Wilkes-Barre/Scranton Penguins", "Penguins", "WBS", Color.of(0xFEC23D)),
		
		ATLANTIC_ALLSTAR(407, "Atlantic Division", "Atlantic Division All-Stars", "Atlantic All-Stars", "ATL", Color.of(0x95D0DE)),
		CENTRAL_ALLSTAR(409, "Central Division", "Central Division All-Stars", "Central All-Stars", "CEN", Color.of(0xC9B998)),
		NORTH_ALLSTAR(408, "North Division", "North Division All-Stars", "Pacific All-Stars", "NOR", Color.of(0x09122F)),
		PACIFIC_ALLSTAR(410, "Pacific Division", "Pacific Division All-Stars", "Pacific All-Stars", "PAC", Color.of(0xCC1A32));
		
		private static final Logger LOGGER = LoggerFactory.getLogger(Team.class);
	
		private final int id;
		private final String locationName;
		private final String fullName;
		private final String nickname;
		private final String teamCode;
		private final Color color;

		private Team(int id, String locationName, String fullName, String nickname, String teamCode, Color color) {
			this.id = id;
			this.locationName = locationName;
			this.fullName = fullName;
			this.nickname = nickname;
			this.teamCode = teamCode;
			this.color = color;
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

		public Color getColor() {
			return color;
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