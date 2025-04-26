package com.hazeluff.discord.nhl;

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

import org.bson.BsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.gdc.nhl.NHLGameDayChannelsManager;
import com.hazeluff.discord.nhl.NHLTeams.Team;
import com.hazeluff.discord.utils.HttpException;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.NHLGateway;
import com.hazeluff.nhl.game.Game;
import com.hazeluff.nhl.game.GameType;

/**
 * This class is used to start GameTrackers for games and to maintain the channels in discord for those games.
 */
public class NHLGameScheduler extends Thread {

	private static final Logger LOGGER = LoggerFactory.getLogger(NHLGameScheduler.class);
	
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
			if (g1.getGameId() == g2.getGameId()) {
				return 0;
			}
			int diff = g1.getStartTime().compareTo(g2.getStartTime());
			return diff == 0 ? Integer.compare(g1.getGameId(), g2.getGameId()) : diff;
		}
	};

	private final Map<Game, NHLGameTracker> activeNHLGameTrackers;
	private final Map<Game, NHLGameTracker> fourNationsGameTrackers;

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
	NHLGameScheduler(Map<Integer, Game> games, Map<Game, NHLGameTracker> activeNHLGameTrackers,
			Map<Game, NHLGameTracker> fourNationsGameTrackers) {
		this.games = games;
		this.activeNHLGameTrackers = activeNHLGameTrackers;
		this.fourNationsGameTrackers = fourNationsGameTrackers;
	}

	public NHLGameScheduler() {
		activeNHLGameTrackers = new ConcurrentHashMap<>();
		fourNationsGameTrackers = new ConcurrentHashMap<>();
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
		return buildGames(NHLGateway.getAllTeamRawGames());
	}

	static Map<Integer, Game> buildGames(Map<Integer, BsonDocument> rawMap) {
		LOGGER.info("Build Games...");
		return rawMap.entrySet()
	        .stream()
	        .map(e -> buildGame(e.getValue()))
	        .filter(Objects::nonNull)
			.collect(Collectors.toConcurrentMap(
					game -> game.getGameId(),
					game -> game));
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
		activeGames.forEach(game -> createNHLGameTracker(game));

		// Four Nation games
		createFourNationsGameTrackers();
	}


	/**
	 * Updates the game schedule and adds games in a recent time frame to the list
	 * of games.
	 * 
	 * @throws HttpException
	 */
	void updateGameSchedule() throws HttpException {
		LOGGER.info("Updating game schedule.");

		Map<Integer, BsonDocument> jsonFetchedGames = NHLGateway.getAllTeamRawGames();
		jsonFetchedGames.entrySet().stream().forEach(jsonFetchedGame -> {
			int gamePk = jsonFetchedGame.getKey();
			BsonDocument jsonSchedule = jsonFetchedGame.getValue();
			if (!games.containsKey(gamePk)) {
				// Create a new game object and put it in our map
				Game newGame = Game.parse(jsonSchedule);
				if (newGame != null) {
					// Games can be null if they fail to parse.
					// Only put non-null values (map throws error otherwise).
					games.put(gamePk, newGame);
				} else {
					LOGGER.debug("Fetched game could not be parsed. Skipping insertion...");
				}
			} else {
				games.get(gamePk).updateSchedule(jsonSchedule);
			}
		});

		// Remove from stored games if it isn't in the list of games fetched
		this.games.entrySet().removeIf(entry -> !jsonFetchedGames.containsKey(entry.getKey()));
	}

	/**
	 * Removes finished trackers, and starts trackers for active games.
	 */
	void updateTrackers() {
		removeInactiveNHLGames();
		createNHLGameTrackers();

		removeInactiveFourNationsGames();
		createFourNationsGameTrackers();
	}

	public void removeInactiveNHLGames() {
		LOGGER.info("Removing finished NHL trackers.");
		activeNHLGameTrackers.entrySet().removeIf(map -> {
			NHLGameTracker gameTracker = map.getValue();
			int gamePk = gameTracker.getGame().getGameId();
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

	public void createNHLGameTrackers() {
		LOGGER.info("Starting new trackers for NHL games.");
		for (Team team : Team.values()) {
			if (team.isNHLTeam()) {
				getActiveGames(team).forEach(activeGame -> {
					createNHLGameTracker(activeGame);
				});
			}
		}
	}
	
	/*
	 * Playoffs
	 */
	public List<Game> getPlayoffGames() {
		return games.entrySet().stream()
			.map(Entry::getValue)
			.filter(game -> game.getGameType().equals(GameType.PLAYOFF))
			.collect(Collectors.toList());
	}
	
	public List<Game> getActivePlayoffGames(Team team) {
		List<Game> playoffGames = getPlayoffGames();
		List<Game> games = new ArrayList<>();
		games.addAll(getNearestGames(getPastGames(playoffGames.stream(), team), 1));
		Game currentGame = getCurrentLiveGame(getPlayoffGames().stream(), team);
		if (currentGame != null) {
			games.add(currentGame);
		} else {
			games.addAll(getNearestGames(getFutureGames(playoffGames.stream(), team), 1));
		}
		return games;
	}

	public List<Game> getActivePlayoffGames(List<Team> teams) {
		return new ArrayList<>(new HashSet<>(teams.stream()
				.map(team -> getActivePlayoffGames(team))
				.flatMap(Collection::stream)
				.collect(Collectors.toList())));
	}

	public List<Game> getActivePlayoffGames() {
		return getActivePlayoffGames(NHLTeams.getSortedValues());
	}
	

	/*
	 * Four Nations
	 */

	public void createFourNationsGameTrackers() {
		LOGGER.info("Starting new trackers for Four Nations games.");
		for (Game game : getFourNationsGames()) {
			createFourNationsGameTracker(game);
		}
	}

	public List<Game> getFourNationsGames() {
		return games.entrySet().stream()
				.map(Entry::getValue)
				.filter(game -> game.getGameType().isFourNations())
				.collect(Collectors.toList());
	}

	public NHLGameTracker getFourNationsGameTracker(Game game) {
		return fourNationsGameTrackers.get(game);
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
	private void createFourNationsGameTracker(Game game) {
		if (!fourNationsGameTrackers.containsKey(game)) {
			LOGGER.info("Creating GameTracker: " + game.getGameId());
			NHLGameTracker newGameTracker = NHLGameTracker.get(game);
			fourNationsGameTrackers.put(game, newGameTracker);
		} else {
			LOGGER.debug("GameTracker already exists: " + game.getGameId());
		}
	}

	public void removeInactiveFourNationsGames() {
		LOGGER.info("Removing finished Four Nations trackers.");
		fourNationsGameTrackers.entrySet().removeIf(map -> {
			NHLGameTracker gameTracker = map.getValue();
			int gamePk = gameTracker.getGame().getGameId();
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
				.filter(game -> !game.getGameState().isStarted())
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
				.filter(game -> game.getGameState().isFinished())
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
				.filter(game -> game.getGameState().isLive())
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
	 * Searches all games and returns the NHLGame that would produce the same
	 * channel name as the parameter.
	 * 
	 * @param channelName
	 *            name of the Discord channel
	 * @return NHLGame that produces the same channel name<br>
	 *         null if game cannot be found; null if class is not initialized
	 * @throws NHLGameSchedulerException
	 */
	public Game getGameByChannelName(String channelName) {
		return games.entrySet()
				.stream()
				.map(Entry::getValue)
				.filter(game -> NHLGameDayChannelsManager.buildChannelName(game).equalsIgnoreCase(channelName))
				.findAny()
				.orElse(null);
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
	public NHLGameTracker getGameTracker(Game game) {
		return activeNHLGameTrackers.get(game);
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
	private void createNHLGameTracker(Game game) {
		if (!activeNHLGameTrackers.containsKey(game)) {
			LOGGER.info("Creating GameTracker: " + game.getGameId());
			NHLGameTracker newGameTracker = NHLGameTracker.get(game);
			activeNHLGameTrackers.put(game, newGameTracker);
		} else {
			LOGGER.debug("GameTracker already exists: " + game.getGameId());
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

	public boolean isGameActive(Team team, String channelName) {
		return getActiveGames(team).stream()
				.anyMatch(game -> channelName.equalsIgnoreCase(NHLGameDayChannelsManager.buildChannelName(game)));
	}

	public boolean isGameActive(List<Team> teams, String channelName) {
		return getActiveGames(teams).stream()
				.anyMatch(game -> channelName.equalsIgnoreCase(NHLGameDayChannelsManager.buildChannelName(game)));
	}

	Map<Game, NHLGameTracker> getActiveGameTrackers() {
		return new HashMap<>(activeNHLGameTrackers);
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

	public NHLGameTracker toGameTracker(Game game) {
		return NHLGameTracker.get(game);
	}

	public boolean isGameExist(Game game) {
		return games.containsKey(game.getGameId());
	}
}
