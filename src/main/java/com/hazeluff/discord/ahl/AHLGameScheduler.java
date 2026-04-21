package com.hazeluff.discord.ahl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.ahl.AHLGateway;
import com.hazeluff.ahl.game.AHLGame;
import com.hazeluff.discord.Config;
import com.hazeluff.discord.ahl.AHLTeams.Team;
import com.hazeluff.discord.bot.SchedulerException;
import com.hazeluff.discord.utils.HttpException;
import com.hazeluff.discord.utils.Utils;

/**
 * This class is used to start GameTrackers for games and to maintain the channels in discord for those games.
 */
public class AHLGameScheduler extends Thread {

	private static final Logger LOGGER = LoggerFactory.getLogger(AHLGameScheduler.class);
	
	static final long GAME_SCHEDULE_UPDATE_RATE = 43200000L;

	// Poll for if the day has rolled over every 30 minutes
	static final long UPDATE_RATE = 1800000L;
	static final long RETRY_RATE = 300000L;

	private Map<Integer, AHLGame> regularGames = new ConcurrentHashMap<>();
	private Map<Integer, AHLGame> playoffGames = new ConcurrentHashMap<>();
	private AtomicBoolean init = new AtomicBoolean(false);

	/**
	 * To be applied to the overall games list, so that it removes duplicates and sorts all games in order. Duplicates
	 * are determined by the gamePk being identicle. Games are sorted by game date.
	 */
	static final Comparator<AHLGame> GAME_COMPARATOR = new Comparator<AHLGame>() {
		@Override
		public int compare(AHLGame g1, AHLGame g2) {
			if (g1.getId() == g2.getId()) {
				return 0;
			}
			int diff = g1.getDate().compareTo(g2.getDate());
			return diff == 0 ? Integer.compare(g1.getId(), g2.getId()) : diff;
		}
	};

	private final Map<AHLGame, AHLGameTracker> activeGameTrackers;

	AtomicReference<LocalDate> lastUpdate = new AtomicReference<>();

	/**
	 * Constructor for injecting private members (Use only for testing).
	 * 
	 * @param discordmanager
	 * @param games
	 * @param activeGameTrackers
	 * @param teamSubscriptions
	 * @param teamLatestGames
	 */
	AHLGameScheduler(Map<Integer, AHLGame> regularGames, Map<Integer, AHLGame> games, 
			Map<AHLGame, AHLGameTracker> activeGameTrackers) {
		this.regularGames = regularGames;
		this.activeGameTrackers = activeGameTrackers;
	}

	public AHLGameScheduler() {
		activeGameTrackers = new ConcurrentHashMap<>();
	}


	/**
	 * Starts the thread that sets up channels and polls for updates to NHLGameTrackers.
	 */
	@Override
	public void run() {
		LOGGER.info("Initializing...");
		try {
			/*
			 * Initialize games, trackers, guild channels.
			 */
			initGames();
			initTrackers();

		} catch (HttpException e) {
			LOGGER.error("Error occured when initializing games.", e);
			throw new RuntimeException(e);
		}

		init.set(true);
		LOGGER.info("Finished Initializing.");

		lastUpdate.set(Utils.getCurrentDate(Config.SERVER_ZONE));
		while (!isStop()) {
			LOGGER.info("Checking for update [lastUpdate={}]", getLastUpdate().toString());
			LocalDate today = Utils.getCurrentDate(Config.SERVER_ZONE);
			try {
				if (today.compareTo(getLastUpdate()) > 0) {
					LOGGER.info("New day detected [today={}]. Updating schedule and trackers...", today.toString());
					updateGameSchedule();
					updateTrackers();
					lastUpdate.set(today);
					LOGGER.info("Successfully updated games.");
				}
				Utils.sleep(UPDATE_RATE);
			} catch (Exception e) {
				LOGGER.error("Error occured when updating games.", e);
				Utils.sleep(RETRY_RATE);
			}
		}
	}

	static BsonArray getRegularSeasonSchedule() {
		return AHLGateway.getSchedule(-1, Config.AHL_CURRENT_SEASON.getSeasonId(), -1);
	}

	static BsonArray getPlayoffSeasonSchedule() {
		return AHLGateway.getSchedule(-1, Config.AHL_CURRENT_SEASON.getSeasonPlayoffId(), -1);
	}

	/**
	 * Gets game information from NHL API and initializes creates Game objects for
	 * them.
	 * 
	 * @throws HttpException
	 */
	public void initGames() throws HttpException {
		LOGGER.info("Initializing Games...");
		// Retrieve schedule/game information from NHL API
		this.regularGames = initAllRegularGames();
		LOGGER.info("Retrieved all regular games: [" + regularGames.size() + "]");
		this.playoffGames = initAllPlayoffGames();
		LOGGER.info("Retrieved all playoff games: [" + playoffGames.size() + "]");
		LOGGER.info("Finished Initialization.");
	}

	static Map<Integer, AHLGame> initAllRegularGames() {
		LOGGER.info("Initializing All Regular Games...");
		BsonArray jsonRegularScheduleGames = getRegularSeasonSchedule();
		if (jsonRegularScheduleGames == null)
			throw new RuntimeException("Could not fetch AHL Regular Schedule");
		return buildGames(jsonRegularScheduleGames);
	}

	static Map<Integer, AHLGame> initAllPlayoffGames() {
		LOGGER.info("Initializing All Playoff Games...");
		BsonArray jsonPlayoffScheduleGames = getPlayoffSeasonSchedule();
		if (jsonPlayoffScheduleGames == null)
			throw new RuntimeException("Could not fetch AHL Playoff Schedule");
		return buildGames(jsonPlayoffScheduleGames);
	}

	static Map<Integer, AHLGame> buildGames(BsonArray rawArray) {
		LOGGER.info("Build Games...");
		return rawArray.stream()
	        .map(BsonValue::asDocument)
	        .map(AHLGame::parse)
	        .filter(Objects::nonNull)
				.collect(Collectors.toConcurrentMap(game -> game.getId(), game -> game));
	}

	static AHLGame buildGame(BsonDocument jsonGame) {
		AHLGame game = AHLGame.parse(jsonGame);
		return game;
	}

	/**
	 * Create GameTrackers for each game in the list if they are not ended.
	 * 
	 * @param list
	 *            list of games to start trackers for.
	 */
	void initTrackers() {
		LOGGER.info("Creating trackers.");
		// Regular games
		Set<AHLGame> games = new TreeSet<>(GAME_COMPARATOR);
		for (Team team : Team.values()) {
			games.addAll(getActiveGames(team));
		}
		// Regular games
		for (Team team : Team.values()) {
			games.addAll(getActivePlayoffGames(team));
		}
		games.forEach(game -> createGameTracker(game));
	}


	/**
	 * Updates the game schedule and adds games in a recent time frame to the list
	 * of games.
	 * 
	 * @throws HttpException
	 */
	void updateGameSchedule() throws HttpException {
		LOGGER.info("Updating game schedule.");
		updateRegularGameSchedule();
		updatePlayoffGameSchedule();
	}

	void updateRegularGameSchedule() {
		LOGGER.info("Updating regular game schedule.");
		updateGamesMap(this.regularGames, getRegularSeasonSchedule());
	}

	void updatePlayoffGameSchedule() {
		LOGGER.info("Updating playoff game schedule.");
		updateGamesMap(this.playoffGames, getPlayoffSeasonSchedule());
	}

	void updateGamesMap(Map<Integer, AHLGame> games, BsonArray newGamesArray) {
		if (newGamesArray == null)
			throw new SchedulerException("`newGamesArray` was null");

		newGamesArray.stream()
			.map(BsonValue::asDocument)
			.forEach(jsonScheduleGame -> {
				int gameId = AHLGame.parseId(jsonScheduleGame);
				if (!games.containsKey(gameId)) {
					// Create a new game object and put it in our map
					AHLGame newGame = AHLGame.parse(jsonScheduleGame);
					if (newGame != null) {
						// Games can be null if they fail to parse.
						// Only put non-null values (map throws error otherwise).
						games.put(gameId, newGame);
					} else {
						LOGGER.warn("Fetched game could not be parsed. Skipping insertion...");
					}
				}
			});

		// Remove from stored games if it isn't in the list of games fetched
		games.entrySet()
				.removeIf(
						entry -> !newGamesArray.stream()
						.map(BsonValue::asDocument)
						.map(jsonGame -> AHLGame.parseId(jsonGame))
						.anyMatch(id -> entry.getKey().equals(id))
				);
	}

	/**
	 * Removes finished trackers, and starts trackers for active games.
	 */
	void updateTrackers() {
		removeInactiveGames();
		createRegularGameTrackers();
		createPlayoffGameTrackers();
	}

	public void removeInactiveGames() {
		LOGGER.info("Removing finished NHL trackers.");
		activeGameTrackers.entrySet().removeIf(map -> {
			AHLGameTracker gameTracker = map.getValue();
			int gameId = gameTracker.getGame().getId();
			if (!regularGames.containsKey(gameId) && !playoffGames.containsKey(gameId)) {
				LOGGER.info("Game is has been removed: " + gameId);
				gameTracker.interrupt();
				return true;
			} else if (gameTracker.isFinished()) {
				LOGGER.info("Game is finished: " + gameTracker.getGame());
				gameTracker.interrupt();
				return true;
			} else {
				return false;
			}
		});
	}

	public void createRegularGameTrackers() {
		LOGGER.info("Starting new trackers for regular games.");
		for (Team team : AHLTeams.getSortedValues()) {
			getActiveGames(team).forEach(activeGame -> {
				createGameTracker(activeGame);
			});
		}
	}

	public void createPlayoffGameTrackers() {
		LOGGER.info("Starting new trackers for playoff games.");
		for (Team team : AHLTeams.getSortedValues()) {
			getActivePlayoffGames(team).forEach(activeGame -> {
				createGameTracker(activeGame);
			});
		}
	}
	
	/*
	 * Game Schedule Utils
	 */
	static AHLGame getNextGame(List<AHLGame> games) {
		if (games.isEmpty()) {
			return null;
		}
		return games.get(0);
	}

	static List<AHLGame> getFutureGames(Stream<AHLGame> gameStream, Team team) {
		return gameStream
				.sorted(GAME_COMPARATOR)
				.filter(game -> team == null || game.containsTeam(team))
				.filter(game -> game.getDate().isAfter(LocalDate.now()))
				.collect(Collectors.toList());
	}

	static List<AHLGame> getPastGames(Stream<AHLGame> gameStream, Team team) {
		return gameStream.sorted(GAME_COMPARATOR.reversed())
				.filter(game -> team == null || game.containsTeam(team))
				.filter(game -> game.getDate().isBefore(LocalDate.now()))
				.collect(Collectors.toList());
	}

	static AHLGame getLastGame(List<AHLGame> games) {
		if (games.isEmpty()) {
			return null;
		}
		return games.get(0);
	}

	static List<AHLGame> getNearestGames(List<AHLGame> games, int numGames) {
		if (games.isEmpty()) {
			return games;
		}
		if (numGames > games.size()) {
			numGames = games.size();
		}
		return games.subList(0, numGames);
	}

	public static AHLGame getCurrentLiveGame(Stream<AHLGame> gameStream, Team team) {
		return gameStream
				.filter(game -> team == null ? true : game.containsTeam(team))
				.filter(game -> game.getDate().isEqual(LocalDate.now()))
				.findAny()
				.orElse(null);
	}

	/*
	 * Regular Games
	 */
	/**
	 * Gets the latest (up to) 2 games to be used as channels in a guild. The channels can consists of the following
	 * games (priority in order).
	 * <ol>
	 * <li>Last Game</li>
	 * <li>Current Game</li>
	 * <li>Future Games</li>
	 * </ol>
	 * 
	 * @param team
	 *            team to get games of
	 * @return list of active/active games
	 */
	public List<AHLGame> getActiveGames(Team team) {
		List<AHLGame> games = new ArrayList<>();
		games.addAll(getPastGames(team, 1));
		AHLGame currentGame = getCurrentLiveGame(team);
		if (currentGame != null) {
			games.add(currentGame);
		} else {
			games.addAll(getFutureGames(team, 1));
		}
		return games;
	}

	/**
	 * Gets the latest games for multiple teams.
	 * 
	 * @param teams
	 * @return
	 */
	public List<AHLGame> getActiveGames(List<Team> teams) {
		return new ArrayList<>(new HashSet<>(teams.stream()
				.map(team -> getActiveGames(team))
				.flatMap(Collection::stream)
				.collect(Collectors.toList())));
	}

	/**
	 * Gets the current game for the provided team
	 * 
	 * @param team
	 *            team to get current game for
	 * @return
	 */
	public AHLGame getCurrentLiveGame(Team team) {
		return getCurrentLiveGame(this.regularGames.entrySet().stream().map(Entry::getValue), team);
	}

	public List<AHLGame> getFutureGames(Team team) {
		return getFutureGames(this.regularGames.entrySet().stream().map(Entry::getValue), team);
	}
	
	public List<AHLGame> getFutureGames(Team team, int numGames) {
		return getNearestGames(getFutureGames(team), numGames);

	}

	public AHLGame getNextGame(Team team) {
		return getNextGame(getFutureGames(team));
	}

	public List<AHLGame> getPastGames(Team team) {
		return getPastGames(this.regularGames.entrySet().stream().map(Entry::getValue), team);
	}

	public List<AHLGame> getPastGames(Team team, int numGames) {
		return getNearestGames(getPastGames(team), numGames);
	}

	public Set<AHLGame> getRegularGames() {
		return new HashSet<>(regularGames.values());
	}
	
	public List<AHLGame> getRegularGames(Team team) {
		return getRegularGames().stream()
				.filter(game -> game.getTeams().contains(team))
				.collect(Collectors.toList());
	}

	/**
	 * <p>
	 * Gets the last game for the provided team.
	 * </p>
	 * <p>
	 * See {@link #getPastGame(Team, int)}
	 * </p>
	 * 
	 * @param team
	 *            team to get last game for
	 * @return NHLGame of last game for the provided team
	 */
	public AHLGame getLastGame(Team team) {
		return getLastGame(getPastGames(team));

	}

	/**
	 * Gets the existing GameTracker for the specified game, if it exists. If the
	 * GameTracker does not exist, a new one is created.
	 * 
	 * @param game
	 *            game to find NHLGameTracker for
	 * @return NHLGameTracker for the game, if it exists <br>
	 *         null, if it does not exists
	 * 
	 */
	public AHLGameTracker getGameTracker(AHLGame game) {
		return activeGameTrackers.get(game);
	}

	/**
	 * Creates and caches a GameTracker for the given game.
	 * 
	 * @param game
	 *            game to find NHLGameTracker for
	 * @return NHLGameTracker for the game, if it exists <br>
	 *         null, if it does not exists
	 * 
	 */
	private void createGameTracker(AHLGame game) {
		if (!activeGameTrackers.containsKey(game)) {
			LOGGER.info("Creating GameTracker: " + game.getId());
			AHLGameTracker newGameTracker = AHLGameTracker.get(game);
			activeGameTrackers.put(game, newGameTracker);
		} else {
			LOGGER.debug("GameTracker already exists: " + game.getId());
		}
	}
	
	/*
	 * Playoff
	 */
	public List<AHLGame> getActivePlayoffGames(Team team) {
		List<AHLGame> games = new ArrayList<>();
		games.addAll(getPastPlayoffGames(team, 1));
		AHLGame currentGame = getCurrentLivePlayoffGame(team);
		if (currentGame != null) {
			games.add(currentGame);
		} else {
			games.addAll(getFuturePlayoffGames(team, 1));
		}
		return games;
	}

	/**
	 * Gets the current game for the provided team
	 * 
	 * @param team
	 *            team to get current game for
	 * @return
	 */
	public AHLGame getCurrentLivePlayoffGame(Team team) {
		return getCurrentLiveGame(this.playoffGames.entrySet().stream().map(Entry::getValue), team);
	}

	public List<AHLGame> getFuturePlayoffGames(Team team) {
		return getFutureGames(this.playoffGames.entrySet().stream().map(Entry::getValue), team);
	}

	public List<AHLGame> getFuturePlayoffGames(Team team, int numGames) {
		return getNearestGames(getFuturePlayoffGames(team), numGames);

	}

	public AHLGame getNextPlayoffGame(Team team) {
		return getNextGame(getFuturePlayoffGames(team));
	}

	public List<AHLGame> getPastPlayoffGames(Team team) {
		return getPastGames(this.playoffGames.entrySet().stream().map(Entry::getValue), team);
	}

	public List<AHLGame> getPastPlayoffGames(Team team, int numGames) {
		return getNearestGames(getPastPlayoffGames(team), numGames);
	}

	public Set<AHLGame> getPlayoffGames() {
		return new HashSet<>(playoffGames.values());
	}
	
	public List<AHLGame> getPlayoffGames(Team team) {
		return getPlayoffGames().stream()
				.filter(game -> game.getTeams().contains(team))
				.collect(Collectors.toList());
	}

	/**
	 * Gets a list games for a given team. An inactive game is one that is not in the teamLatestGames map/list.
	 * 
	 * @param team
	 *            team to get inactive games of
	 * @return list of inactive games
	 */
	List<AHLGame> getInactiveGames(Team team) {
		return regularGames.entrySet().stream().map(
				Entry::getValue)
				.filter(game -> game.containsTeam(team))
				.filter(game -> team == null ? true : game.containsTeam(team))
				.filter(game -> !getActiveGames(team).contains(game))
				.collect(Collectors.toList());
	}

	Map<AHLGame, AHLGameTracker> getActiveGameTrackers() {
		return new HashMap<>(activeGameTrackers);
	}

	/**
	 * Used for stubbing the loop of {@link #run()} for tests.
	 * 
	 * @return
	 */
	boolean isStop() {
		return false;
	}

	public void setInit(boolean value) {
		init.set(value);
	}

	public boolean isInit() {
		return init.get();
	}

	public LocalDate getLastUpdate() {
		return lastUpdate.get();
	}

	public AHLGameTracker toGameTracker(AHLGame game) {
		return AHLGameTracker.get(game);
	}
}
