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

	private Map<Integer, Game> games = new ConcurrentHashMap<>();
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

	private final Map<Game, AHLGameTracker> activeRegularGameTrackers;

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
	AHLGameScheduler(Map<Integer, Game> games, Map<Game, AHLGameTracker> activeNHLGameTrackers) {
		this.games = games;
		this.activeRegularGameTrackers = activeNHLGameTrackers;
	}

	public AHLGameScheduler() {
		activeRegularGameTrackers = new ConcurrentHashMap<>();
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

		lastUpdate.set(Utils.getCurrentDate(Config.DATE_START_TIME_ZONE));
		while (!isStop()) {
			LOGGER.info("Checking for update [lastUpdate={}]", getLastUpdate().toString());
			LocalDate today = Utils.getCurrentDate(Config.DATE_START_TIME_ZONE);
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

	/**
	 * Gets game information from NHL API and initializes creates Game objects for
	 * them.
	 * 
	 * @throws HttpException
	 */
	public void initGames() throws HttpException {
		LOGGER.info("Initializing Games...");
		// Retrieve schedule/game information from NHL API
		this.games = initAllTeamGames();
		LOGGER.info("Retrieved all games: [" + games.size() + "]");
		LOGGER.info("Finished Initialization.");
	}

	static Map<Integer, Game> initAllTeamGames() {
		LOGGER.info("Initializing All Team Games...");
		return buildGames(AHLGateway.getSchedule(-1, Config.AHL_CURRENT_SEASON.getSeasonId(), -1));
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
		// NHL games
		Set<Game> activeGames = new TreeSet<>(GAME_COMPARATOR);
		for (Team team : Team.values()) {
			activeGames.addAll(getActiveGames(team));
		}
		activeGames.forEach(game -> createRegularGameTracker(game));
	}


	/**
	 * Updates the game schedule and adds games in a recent time frame to the list
	 * of games.
	 * 
	 * @throws HttpException
	 */
	void updateGameSchedule() throws HttpException {
		LOGGER.info("Updating game schedule.");

		BsonArray jsonScheduleGames = AHLGateway.getSchedule(-1, Config.AHL_CURRENT_SEASON.getSeasonId(), -1);
		jsonScheduleGames.stream()
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
		this.games.entrySet()
				.removeIf(
					entry -> !jsonScheduleGames.stream()
						.map(BsonValue::asDocument)
						.map(jsonGame -> Game.parseId(jsonGame))
						.anyMatch(id -> entry.getKey().equals(id))
				);
	}

	/**
	 * Removes finished trackers, and starts trackers for active games.
	 */
	void updateTrackers() {
		removeInactiveRegularGames();
		createRegularGameTrackers();
	}

	public void removeInactiveRegularGames() {
		LOGGER.info("Removing finished NHL trackers.");
		activeRegularGameTrackers.entrySet().removeIf(map -> {
			AHLGameTracker gameTracker = map.getValue();
			int gamePk = gameTracker.getGame().getId();
			if (!games.containsKey(gamePk)) {
				LOGGER.info("Game is has been removed: " + gamePk);
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
		LOGGER.info("Starting new trackers for NHL games.");
		for (Team team : AHLTeams.getSortedValues()) {
			getActiveGames(team).forEach(activeGame -> {
				createRegularGameTracker(activeGame);
			});
		}
	}

	/*
	 * All Games
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

	private static List<Game> getFutureGames(Stream<Game> gameStream, Team team) {
		return gameStream
				.sorted(GAME_COMPARATOR)
				.filter(game -> team == null || game.containsTeam(team))
				.filter(game -> !game.isStarted())
				.collect(Collectors.toList());
	}

	public List<Game> getFutureGames(Team team) {
		return getFutureGames(this.games.entrySet().stream().map(Entry::getValue), team);
	}
	
	public List<Game> getFutureGames(Team team, int numGames) {
		return getNearestGames(getFutureGames(team), numGames);

	}

	private static Game getNextGame(List<Game> games) {
		if (games.isEmpty()) {
			return null;
		}
		return games.get(0);
	}

	public Game getNextGame(Team team) {
		return getNextGame(getFutureGames(team));
	}

	private static List<Game> getPastGames(Stream<Game> gameStream, Team team) {
		return gameStream.sorted(GAME_COMPARATOR.reversed())
				.filter(game -> team == null || game.containsTeam(team))
				.filter(game -> game.isFinished())
				.collect(Collectors.toList());
	}

	public List<Game> getPastGames(Team team) {
		return getPastGames(this.games.entrySet().stream().map(Entry::getValue), team);
	}

	private static List<Game> getNearestGames(List<Game> games, int numGames) {
		if (games.isEmpty()) {
			return games;
		}
		if (numGames > games.size()) {
			numGames = games.size();
		}
		return games.subList(0, numGames);
	}

	public List<Game> getPastGames(Team team, int numGames) {
		return getNearestGames(getPastGames(team), numGames);
	}

	private Game getLastGame(List<Game> games) {
		if (games.isEmpty()) {
			return null;
		}
		return games.get(0);
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

	public Game getCurrentLiveGame(Stream<Game> gameStream, Team team) {
		return gameStream
				.filter(game -> team == null ? true : game.containsTeam(team))
				.filter(game -> game.isLive())
				.findAny()
				.orElse(null);
	}

	/**
	 * Gets the current game for the provided team
	 * 
	 * @param team
	 *            team to get current game for
	 * @return
	 */
	public Game getCurrentLiveGame(Team team) {
		return getCurrentLiveGame(this.games.entrySet().stream().map(Entry::getValue), team);
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
		return activeRegularGameTrackers.get(game);
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
	private void createRegularGameTracker(Game game) {
		if (!activeRegularGameTrackers.containsKey(game)) {
			LOGGER.info("Creating GameTracker: " + game.getId());
			AHLGameTracker newGameTracker = AHLGameTracker.get(game);
			activeRegularGameTrackers.put(game, newGameTracker);
		} else {
			LOGGER.debug("GameTracker already exists: " + game.getId());
		}
	}
	
	/**
	 * Gets a list games for a given team. An inactive game is one that is not in the teamLatestGames map/list.
	 * 
	 * @param team
	 *            team to get inactive games of
	 * @return list of inactive games
	 */
	List<Game> getInactiveGames(Team team) {
		return games.entrySet().stream().map(
				Entry::getValue)
				.filter(game -> game.containsTeam(team))
				.filter(game -> team == null ? true : game.containsTeam(team))
				.filter(game -> !getActiveGames(team).contains(game))
				.collect(Collectors.toList());
	}

	Map<Game, AHLGameTracker> getActiveGameTrackers() {
		return new HashMap<>(activeRegularGameTrackers);
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

	public Set<Game> getGames() {
		return new HashSet<>(games.values());
	}
	
	public List<Game> getGames(Team team) {
		return getGames().stream()
				.filter(game -> game.getTeams().contains(team))
				.collect(Collectors.toList());
	}

	public AHLGameTracker toGameTracker(Game game) {
		return AHLGameTracker.get(game);
	}

	public boolean isGameExist(Game game) {
		return games.containsKey(game.getId());
	}
}
