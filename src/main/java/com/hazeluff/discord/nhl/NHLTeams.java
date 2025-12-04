package com.hazeluff.discord.nhl;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.nhl.Division;

import discord4j.rest.util.Color;

public class NHLTeams {

	public static List<Team> getSortedValues() {
		return new ArrayList<>(Team.SORTED_NHL_VALUES);
	}

	public static List<Team> getNonCanucksValues() {
		return new ArrayList<>(Team.SORTED_NHL_VALUES).stream().filter(team -> team != Team.VANCOUVER_CANUCKS)
				.collect(Collectors.toList());
	}

	public enum Team {
		NEW_JERSEY_DEVILS(
				1, 
				"New Jersey", "Devils", 
				"NJD", 
				Division.METRO, 
				"Lets Go Devils!",
				Color.of(0xC8102E),
				ZoneId.of("America/New_York")),
		NEW_YORK_ISLANDERS(
				2, 
				"New York", 
				"Islanders", 
				"NYI", 
				Division.METRO, 
				"Lets Go Islanders!",
				Color.of(0xF26924),
				ZoneId.of("America/New_York")), 
		NEW_YORK_RANGERS(
				3, 
				"New York", 
				"Rangers", 
				"NYR", 
				Division.METRO, 
				"Lets Go Rangers!",
				Color.of(0x0038A8),
				ZoneId.of("America/New_York")),
		PHILADELPHIA_FLYERS(
				4, 
				"Philadelphia", 
				"Flyers", 
				"PHI", 
				Division.METRO, 
				"Lets Go Flyers!",
				Color.of(0xFA4616),
				ZoneId.of("America/New_York")),
		PITTSBURGH_PENGUINS(
				5, 
				"Pittsburgh", 
				"Penguins", 
				"PIT", 
				Division.METRO, 
				"Lets Go Pens!", 
				Color.of(0xFFB81C),
				ZoneId.of("America/New_York")),
		BOSTON_BRUINS(
				6, 
				"Boston", 
				"Bruins", 
				"BOS", 
				Division.ATLANTIC, 
				"Lets Go Bruins!", 
				Color.of(0xFFB81C),
				ZoneId.of("America/New_York")),
		BUFFALO_SABRES(
				7, 
				"Buffalo", 
				"Sabres", 
				"BUF", 
				Division.ATLANTIC, 
				"Lets Go Buffalo!", 
				Color.of(0xFFB81C),
				ZoneId.of("America/New_York")),
		MONTREAL_CANADIENS(
				8, 
				"Montréal", 
				"Canadiens", 
				"MTL", 
				Division.ATLANTIC, 
				"Olé Olé Olé",
				Color.of(0xA6192E),
				ZoneId.of("America/Montreal")),
		OTTAWA_SENATORS(
				9, 
				"Ottawa", 
				"Senators", 
				"OTT", 
				Division.ATLANTIC, 
				"Go Sens Go!", 
				Color.of(0xC8102E),
				ZoneId.of("America/Toronto")),
		TORONTO_MAPLE_LEAFS(
				10, 
				"Toronto", 
				"Maple Leafs", 
				"TOR", 
				Division.ATLANTIC, 
				"Go Leafs Go!",
				Color.of(0x00205B),
				ZoneId.of("America/Toronto")),
		CAROLINA_HURRICANES(
				12, 
				"Carolina", 
				"Hurricanes", 
				"CAR", 
				Division.METRO, 
				"Lets Go Canes!", 
				Color.of(0xCC0000),
				ZoneId.of("America/New_York")),
		FLORIDA_PANTHERS(
				13, 
				"Florida", 
				"Panthers", 
				"FLA", 
				Division.ATLANTIC, 
				"Lets Go Panthers!", 
				Color.of(0xB9975B),
				ZoneId.of("America/New_York")),
		TAMPA_BAY_LIGHTNING(
				14, 
				"Tampa Bay", 
				"Lightning", 
				"TBL", 
				Division.ATLANTIC, 
				"Lets Go Lightning!", 
				Color.of(0x00205B),
				ZoneId.of("America/New_York")),
		WASHINGTON_CAPITALS(
				15, 
				"Washington", 
				"Capitals", 
				"WSH", 
				Division.METRO, 
				"Lets Go Caps!", 
				Color.of(0xC8102E),
				ZoneId.of("America/New_York")),
		CHICAGO_BLACKHAWKS(
				16, 
				"Chicago", 
				"Blackhawks", 
				"CHI", 
				Division.CENTRAL, 
				"Lets Go Hawks!",
				Color.of(0xC8102E),
				ZoneId.of("America/Chicago")),
		DETROIT_RED_WINGS(
				17, 
				"Detroit", 
				"Red Wings",
				"DET",
				Division.ATLANTIC,
				"Lets Go Red Wings!",
				Color.of(0xC8102E),
				ZoneId.of("America/Detroit")),
		NASHVILLE_PREDATORS(
				18, 
				"Nashville", 
				"Predators", 
				"NSH", 
				Division.CENTRAL, 
				"Lets Go Predators!", 
				Color.of(0xFFB81C),
				ZoneId.of("America/Chicago")),
		ST_LOUIS_BLUES(
				19, 
				"St. Louis", 
				"Blues", 
				"STL", 
				Division.CENTRAL, 
				"Lets Go Blues!",
				Color.of(0x003087),
				ZoneId.of("America/Chicago")),
		CALGARY_FLAMES(
				20, 
				"Calgary", 
				"Flames", 
				"CGY", 
				Division.PACIFIC, 
				"Go Flames Go!", 
				Color.of(0xC8102E),
				ZoneId.of("America/Edmonton")),
		COLORADO_AVALANCH(
				21, 
				"Colorado", 
				"Avalanche", 
				"COL", 
				Division.CENTRAL, 
				"Lets Go Colorado!",
				Color.of(0x6F263D),
				ZoneId.of("America/Denver")),
		EDMONTON_OILERS(
				22, 
				"Edmonton",
				"Oilers",
				"EDM",
				Division.PACIFIC,
				"Let go Oilers!", 
				Color.of(0xFC4C02),
				ZoneId.of("America/Edmonton")),
		VANCOUVER_CANUCKS(
				23, 
				"Vancouver", 
				"Canucks", 
				"VAN", 
				Division.PACIFIC, 
				"Go Canucks Go!",
				Color.of(0x00843D),
				ZoneId.of("America/Vancouver")),
		ANAHEIM_DUCKS(
				24, 
				"Anaheim", 
				"Ducks", 
				"ANA", 
				Division.PACIFIC, 
				"Lets Go Ducks!",
				Color.of(0xFC4C02),
				ZoneId.of("America/Los_Angeles")),
		DALLAS_STARS(
				25, 
				"Dallas",
				"Stars",
				"DAL",
				Division.CENTRAL, 
				"Go Stars Go!", 
				Color.of(0x006341),
				ZoneId.of("America/Chicago")),
		LA_KINGS(
				26, 
				"Los Angeles",
				"Kings", 
				"LAK",
				Division.PACIFIC,
				"Go Kings Go!", 
				Color.of(0x000000),
				ZoneId.of("America/Los_Angeles")),
		SAN_JOSE_SHARKS(
				28, 
				"San Jose", 
				"Sharks",
				"SJS", 
				Division.PACIFIC, 
				"Lets Go Sharks!",
				Color.of(0x006272),
				ZoneId.of("America/Los_Angeles")),
		COLUMBUS_BLUE_JACKETS(
				29, 
				"Columbus", 
				"Blue Jackets", 
				"CBJ", 
				Division.METRO, 
				"C B J! C B J!", // WTF Seriously?
				Color.of(0x041E42),
				ZoneId.of("America/Chicago")),
		MINNESOTA_WILD(
				30, 
				"Minnesota",
				"Wild", 
				"MIN", 
				Division.CENTRAL, 
				"Lets Go Wild!", 
				Color.of(0x154734),
				ZoneId.of("America/Chicago")),
		WINNIPEG_JETS(
				52, 
				"Winnipeg", 
				"Jets", 
				"WPG", 
				Division.CENTRAL, 
				"Go Jets Go!", 
				Color.of(0xFFFFFF),
				ZoneId.of("America/Winnipeg")),
		VEGAS_GOLDEN_KNIGHTS(
				54, 
				"Vegas", 
				"Golden Knights", 
				"VGK", 
				Division.PACIFIC, 
				"Go Knights Go!",
				Color.of(0xB9975B),
				ZoneId.of("America/Los_Angeles")),
		SEATTLE_KRAKEN(
				55, 
				"Seattle", 
				"Kraken", 
				"SEA", 
				Division.PACIFIC, 
				"Lets Go Kraken!",
				Color.of(0x639FB6),
				ZoneId.of("America/Los_Angeles")),
		UTAH_HC(
				68,
				"Utah",
				"Mammoth",
				"UTA",
				Division.CENTRAL,
				"Lets Go Utah!",
				Color.of(0x6CACE4),
				ZoneId.of("America/Denver")),
		TEAM_CANADA(
				60,
				"Canada",
				null,
				"CAN",
				null,
				"Go Canada Go!",
				Color.of(0xC70D31),
				ZoneId.of("America/Toronto")),
		TEAM_USA(
				67,
				"USA",
				null,
				"USA",
				null,
				"U-S-A! U-S-A! U-S-A!",
				Color.of(0x053185),
				ZoneId.of("America/Toronto")),
		TEAM_SWEDEN(
				66,
				"Sweden",
				null,
				"SWE",
				null,
				"Lets go Sweden!",
				Color.of(0xFDCC32),
				ZoneId.of("America/Toronto")),
		TEAM_FINLAND(
				62,
				"Finland",
				null,
				"FIN",
				null,
				"Lets go Finland!",
				Color.of(0x0C366F),
				ZoneId.of("America/Toronto"));
	
		private static final Logger LOGGER = LoggerFactory.getLogger(Team.class);
	
		public static final String MULTI_TEAM_CHEER = "Lets Go!";
	
		private final int id;
		private final String locationName;
		private final String name;
		private final String code;
		private final Division division;
		private final String cheer;
		private final Color color;
		private final ZoneId timeZone;
	
		private static final Map<Integer, Team> VALUES_MAP = new HashMap<>();
		private static final Map<String, Team> CODES_MAP = new HashMap<>();
		private static final List<Team> SORTED_NHL_VALUES;
	
		static {
			for (Team t : Team.values()) {
				VALUES_MAP.put(t.id, t);
			}
			for (Team t : Team.values()) {
				CODES_MAP.put(t.code, t);
			}
	
			List<Team> allTeams = new ArrayList<>(EnumSet.allOf(Team.class));
			SORTED_NHL_VALUES = new ArrayList<>(allTeams).stream().filter(team -> team.isNHLTeam())
					.collect(Collectors.toList());
			SORTED_NHL_VALUES.sort((t1, t2) -> t1.getFullName().compareTo(t2.getFullName()));
		}
	
		private Team(int id, String locationName, String name, String code, Division division, String cheer, Color color,
				ZoneId timeZone) {
			this.id = id;
			this.locationName = locationName;
			this.name = name;
			this.code = code;
			this.division = division;
			this.cheer = cheer;
			this.color = color;
			this.timeZone = timeZone;
		}
	
		public int getId() {
			return id;
		}
	
		public String getLocationName() {
			return locationName;
		}
	
		public String getName() {
			if (name == null) {
				return locationName;
			}
			return name;
		}
	
		public String getFullName() {
			if (name == null) {
				return locationName;
			}
			return locationName + " " + name;
		}
	
		public String getCode() {
			return code;
		}
	
		public Division getDivision() {
			return division;
		}
	
		public String getCheer() {
			return cheer;
		}
	
		public Color getColor() {
			return color;
		}
	
		public ZoneId getTimeZone() {
			return timeZone;
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
	
		public boolean isNHLTeam() {
			switch (this) {
			case NEW_JERSEY_DEVILS:
			case NEW_YORK_ISLANDERS:
			case NEW_YORK_RANGERS:
			case PHILADELPHIA_FLYERS:
			case PITTSBURGH_PENGUINS:
			case BOSTON_BRUINS:
			case BUFFALO_SABRES:
			case MONTREAL_CANADIENS:
			case OTTAWA_SENATORS:
			case TORONTO_MAPLE_LEAFS:
			case CAROLINA_HURRICANES:
			case FLORIDA_PANTHERS:
			case TAMPA_BAY_LIGHTNING:
			case WASHINGTON_CAPITALS:
			case CHICAGO_BLACKHAWKS:
			case DETROIT_RED_WINGS:
			case NASHVILLE_PREDATORS:
			case ST_LOUIS_BLUES:
			case CALGARY_FLAMES:
			case COLORADO_AVALANCH:
			case EDMONTON_OILERS:
			case VANCOUVER_CANUCKS:
			case ANAHEIM_DUCKS:
			case DALLAS_STARS:
			case LA_KINGS:
			case SAN_JOSE_SHARKS:
			case COLUMBUS_BLUE_JACKETS:
			case MINNESOTA_WILD:
			case WINNIPEG_JETS:
			case VEGAS_GOLDEN_KNIGHTS:
			case SEATTLE_KRAKEN:
			case UTAH_HC:
				return true;
			default:
				return false;
			}
		}
	
		public boolean isFourNationsTeam() {
			switch (this) {
			case TEAM_CANADA:
			case TEAM_USA:
			case TEAM_SWEDEN:
			case TEAM_FINLAND:
				return true;
			default:
				return false;
			}
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