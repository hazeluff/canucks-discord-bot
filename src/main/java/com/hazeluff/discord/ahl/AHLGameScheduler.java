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
import com.hazeluff.ahl.game.Game;
import com.hazeluff.discord.Config;
import com.hazeluff.discord.ahl.AHLTeams.Team;
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

	private Map<Integer, Game> regularGames = new ConcurrentHashMap<>();
	private Map<Integer, Game> playoffGames = new ConcurrentHashMap<>();
	private AtomicBoolean init = new AtomicBoolean(false);

	/**
	 * To be applied to the overall games list, so that it removes duplicates and sorts all games in order. Duplicates
	 * are determined by the gamePk being identicle. Games are sorted by game date.
	 */
	static final Comparator<Game> GAME_COMPARATOR = new Comparator<Game>() {
		@Override
		public int compare(Game g1, Game g2) {
			if (g1.getId() == g2.getId()) {
				return 0;
			}
			int diff = g1.getDate().compareTo(g2.getDate());
			return diff == 0 ? Integer.compare(g1.getId(), g2.getId()) : diff;
		}
	};

	private final Map<Game, AHLGameTracker> activeGameTrackers;

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
	AHLGameScheduler(Map<Integer, Game> regularGames, Map<Integer, Game> games, 
			Map<Game, AHLGameTracker> activeGameTrackers) {
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
			if (today.compareTo(getLastUpdate()) > 0) {
				LOGGER.info("New day detected [today={}]. Updating schedule and trackers...", today.toString());
				try {
					updateGameSchedule();
					updateTrackers();
					lastUpdate.set(today);
					LOGGER.info("Successfully updated games.");
				} catch (Exception e) {
					LOGGER.error("Error occured when updating games.", e);
				}
			}
			Utils.sleep(UPDATE_RATE);
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
		LOGGER.info("Retrieved all regular games: [" + playoffGames.size() + "]");
		LOGGER.info("Finished Initialization.");
	}

	static Map<Integer, Game> initAllRegularGames() {
		LOGGER.info("Initializing All Regular Games...");
		return buildGames(getRegularSeasonSchedule());
	}

	static Map<Integer, Game> initAllPlayoffGames() {
		LOGGER.info("Initializing All Playoff Games...");
		return buildGames(getPlayoffSeasonSchedule());
	}

	static Map<Integer, Game> buildGames(BsonArray rawArray) {
		LOGGER.info("Build Games...");
		return rawArray.stream()
	        .map(BsonValue::asDocument)
	        .map(Game::parse)
	        .filter(Objects::nonNull)
				.collect(Collectors.toConcurrentMap(game -> game.getId(), game -> game));
	}

	static Game buildGame(BsonDocument jsonGame) {
		Game game = Game.parse(jsonGame);
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
		Set<Game> games = new TreeSet<>(GAME_COMPARATOR);
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
		BsonArray jsonRegularScheduleGames = getRegularSeasonSchedule();
		updateGamesMap(this.regularGames, jsonRegularScheduleGames);
	}

	void updatePlayoffGameSchedule() {
		LOGGER.info("Updating playoff game schedule.");
		BsonArray jsonPlayoffScheduleGames = getPlayoffSeasonSchedule();
		updateGamesMap(this.playoffGames, jsonPlayoffScheduleGames);
	}

	void updateGamesMap(Map<Integer, Game> games, BsonArray newGamesArray) {
		newGamesArray.stream()
			.map(BsonValue::asDocument)
			.forEach(jsonScheduleGame -> {
				int gameId = Game.parseId(jsonScheduleGame);
				if (!games.containsKey(gameId)) {
					// Create a new game object and put it in our map
					Game newGame = Game.parse(jsonScheduleGame);
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
						.map(jsonGame -> Game.parseId(jsonGame))
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
	static Game getNextGame(List<Game> games) {
		if (games.isEmpty()) {
			return null;
		}
		return games.get(0);
	}

	static List<Game> getFutureGames(Stream<Game> gameStream, Team team) {
		return gameStream
				.sorted(GAME_COMPARATOR)
				.filter(game -> team == null || game.containsTeam(team))
				.filter(game -> game.getDate().isAfter(LocalDate.now()))
				.collect(Collectors.toList());
	}

	static List<Game> getPastGames(Stream<Game> gameStream, Team team) {
		return gameStream.sorted(GAME_COMPARATOR.reversed())
				.filter(game -> team == null || game.containsTeam(team))
				.filter(game -> game.getDate().isBefore(LocalDate.now()))
				.collect(Collectors.toList());
	}

	static Game getLastGame(List<Game> games) {
		if (games.isEmpty()) {
			return null;
		}
		return games.get(0);
	}

	static List<Game> getNearestGames(List<Game> games, int numGames) {
		if (games.isEmpty()) {
			return games;
		}
		if (numGames > games.size()) {
			numGames = games.size();
		}
		return games.subList(0, numGames);
	}

	public static Game getCurrentLiveGame(Stream<Game> gameStream, Team team) {
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
	public List<Game> getActiveGames(Team team) {
		List<Game> games = new ArrayList<>();
		games.addAll(getPastGames(team, 1));
		Game currentGame = getCurrentLiveGame(team);
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
	public List<Game> getActiveGames(List<Team> teams) {
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
	public Game getCurrentLiveGame(Team team) {
		return getCurrentLiveGame(this.regularGames.entrySet().stream().map(Entry::getValue), team);
	}

	public List<Game> getFutureGames(Team team) {
		return getFutureGames(this.regularGames.entrySet().stream().map(Entry::getValue), team);
	}
	
	public List<Game> getFutureGames(Team team, int numGames) {
		return getNearestGames(getFutureGames(team), numGames);

	}

	public Game getNextGame(Team team) {
		return getNextGame(getFutureGames(team));
	}

	public List<Game> getPastGames(Team team) {
		return getPastGames(this.regularGames.entrySet().stream().map(Entry::getValue), team);
	}

	public List<Game> getPastGames(Team team, int numGames) {
		return getNearestGames(getPastGames(team), numGames);
	}

	public Set<Game> getRegularGames() {
		return new HashSet<>(regularGames.values());
	}
	
	public List<Game> getRegularGames(Team team) {
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
	public Game getLastGame(Team team) {
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
	public AHLGameTracker getGameTracker(Game game) {
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
	private void createGameTracker(Game game) {
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
	public List<Game> getActivePlayoffGames(Team team) {
		List<Game> games = new ArrayList<>();
		games.addAll(getPastPlayoffGames(team, 1));
		Game currentGame = getCurrentLivePlayoffGame(team);
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
	public Game getCurrentLivePlayoffGame(Team team) {
		return getCurrentLiveGame(this.playoffGames.entrySet().stream().map(Entry::getValue), team);
	}

	public List<Game> getFuturePlayoffGames(Team team) {
		return getFutureGames(this.playoffGames.entrySet().stream().map(Entry::getValue), team);
	}

	public List<Game> getFuturePlayoffGames(Team team, int numGames) {
		return getNearestGames(getFuturePlayoffGames(team), numGames);

	}

	public Game getNextPlayoffGame(Team team) {
		return getNextGame(getFuturePlayoffGames(team));
	}

	public List<Game> getPastPlayoffGames(Team team) {
		return getPastGames(this.playoffGames.entrySet().stream().map(Entry::getValue), team);
	}

	public List<Game> getPastPlayoffGames(Team team, int numGames) {
		return getNearestGames(getPastPlayoffGames(team), numGames);
	}

	public Set<Game> getPlayoffGames() {
		return new HashSet<>(playoffGames.values());
	}
	
	public List<Game> getPlayoffGames(Team team) {
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
	List<Game> getInactiveGames(Team team) {
		return regularGames.entrySet().stream().map(
				Entry::getValue)
				.filter(game -> game.containsTeam(team))
				.filter(game -> team == null ? true : game.containsTeam(team))
				.filter(game -> !getActiveGames(team).contains(game))
				.collect(Collectors.toList());
	}

	Map<Game, AHLGameTracker> getActiveGameTrackers() {
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

	public AHLGameTracker toGameTracker(Game game) {
		return AHLGameTracker.get(game);
	}
}
