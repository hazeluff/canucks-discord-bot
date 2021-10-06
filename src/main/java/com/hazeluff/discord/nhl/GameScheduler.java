package com.hazeluff.discord.nhl;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.GameDayChannel;
import com.hazeluff.discord.nhl.Seasons.Season;
import com.hazeluff.discord.utils.HttpException;
import com.hazeluff.discord.utils.HttpUtils;
import com.hazeluff.discord.utils.Utils;
import com.hazeluff.nhl.Game;
import com.hazeluff.nhl.Team;

/**
 * This class is used to start GameTrackers for games and to maintain the channels in discord for those games.
 */
public class GameScheduler extends Thread {

	private static final Logger LOGGER = LoggerFactory.getLogger(GameScheduler.class);

	private Season currentSeason = Config.CURRENT_SEASON;
	
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
			if (g1.getGamePk() == g2.getGamePk()) {
				return 0;
			}
			int diff = g1.getDate().compareTo(g2.getDate());
			return diff == 0 ? Integer.compare(g1.getGamePk(), g2.getGamePk()) : diff;
		}
	};

	private final Map<Game, GameTracker> activeGameTrackers;

	LocalDate lastUpdate;

	/**
	 * Constructor for injecting private members (Use only for testing).
	 * 
	 * @param discordmanager
	 * @param games
	 * @param activeGameTrackers
	 * @param teamSubscriptions
	 * @param teamLatestGames
	 */
	GameScheduler(Map<Integer, Game> games, Map<Game, GameTracker> activeGameTrackers) {
		this.games = games;
		this.activeGameTrackers = activeGameTrackers;
	}

	public GameScheduler() {
		activeGameTrackers = new ConcurrentHashMap<>();
	}


	/**
	 * Starts the thread that sets up channels and polls for updates to NHLGameTrackers.
	 */
	@Override
	public void run() {
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

		lastUpdate = Utils.getCurrentDate(Config.DATE_START_TIME_ZONE);
		while (!isStop()) {
			LocalDate today = Utils.getCurrentDate(Config.DATE_START_TIME_ZONE);
			if (today.compareTo(lastUpdate) > 0) {
				try {
					updateGameSchedule();
					updateTrackers();
					lastUpdate = today;

				} catch (HttpException e) {
					LOGGER.error("Error occured when updating games.", e);
					throw new RuntimeException(e);
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
		LOGGER.info("Initializing");
		// Retrieve schedule/game information from NHL API
		Map<Integer, Game> games = getRawGames(currentSeason.getStartDate(), currentSeason.getEndDate())
				.entrySet()
		        .stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> Game.parse(e.getValue())));
		LOGGER.info("Retrieved all games: [" + games.size() + "]");

		LOGGER.info("Finished Initialization.");
	}

	/**
	 * Create GameTrackers for each game in the list if they are not ended.
	 * 
	 * @param list
	 *            list of games to start trackers for.
	 */
	void initTrackers() {
		LOGGER.info("Creating trackers.");
		Set<Game> activeGames = new TreeSet<>(GAME_COMPARATOR);
		for (Team team : Team.values()) {
			activeGames.addAll(getActiveGames(team));
		}
		activeGames.forEach(game -> getGameTracker(game));
	}


	/**
	 * Updates the game schedule and adds games in a recent time frame to the list
	 * of games.
	 * 
	 * @throws HttpException
	 */
	void updateGameSchedule() throws HttpException {
		LOGGER.info("Updating game schedule.");

		Map<Integer, JSONObject> fetchedGames = getRawGames(currentSeason.getStartDate(), currentSeason.getEndDate());
		fetchedGames.entrySet().stream().forEach(fetchedGame -> {
			int gamePk = fetchedGame.getKey();
			Game existingGame = games.get(gamePk);
			if (existingGame == null) {
				// Create a new game object and put it in our map
				games.put(gamePk, Game.parse(fetchedGame.getValue()));
			} else {
				existingGame.updateGameData(fetchedGame.getValue());
			}
		});

		// Remove the game if it isn't in the list of games fetched
		games.entrySet().removeIf(entry -> !fetchedGames.containsKey(entry.getKey()));
	}

	/**
	 * Removes finished trackers, and starts trackers for active games.
	 */
	void updateTrackers() {
		LOGGER.info("Removing finished trackers.");
		activeGameTrackers.entrySet().removeIf(map -> {
			GameTracker gameTracker = map.getValue();
			int gamePk = gameTracker.getGame().getGamePk();
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

		LOGGER.info("Starting new trackers and creating channels.");
		for (Team team : Team.values()) {
			getActiveGames(team).forEach(activeGame -> {
				getGameTracker(activeGame);
			});
		}
	}

	Map<Integer, JSONObject> getRawGames(ZonedDateTime startDate, ZonedDateTime endDate) throws HttpException {
		return getRawGames(startDate, endDate, null);
	}

	Map<Integer, JSONObject> getRawGames(ZonedDateTime startDate, ZonedDateTime endDate, Team team)
			throws HttpException {
		LOGGER.info("Retrieving games of [" + team + "]");
		String strStartDate = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String strEndDate = endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

		URI uri;
		try {
			URIBuilder uriBuilder = new URIBuilder(Config.NHL_API_URL + "/schedule");
			uriBuilder.addParameter("startDate", strStartDate);
			uriBuilder.addParameter("endDate", strEndDate);
			if (team != null) {
				uriBuilder.addParameter("teamId", String.valueOf(team.getId()));
			}
			uriBuilder.addParameter("expand", "schedule.scoringplays");
			uri = uriBuilder.build();
		} catch (URISyntaxException e) {
			String message = "Error building URI";
			RuntimeException runtimeException = new RuntimeException(message, e);
			LOGGER.error(message, runtimeException);
			throw runtimeException;
		}

		String strJSONSchedule = HttpUtils.getAndRetry(uri, 5, 10000l, "Get Game Schedule.");
		Map<Integer, JSONObject> games = new HashMap<>();
		JSONObject jsonSchedule = new JSONObject(strJSONSchedule);
		JSONArray jsonDates = jsonSchedule.getJSONArray("dates");
		for (int d = 0; d < jsonDates.length(); d++) {
			JSONArray gamesJSONArray = jsonDates.getJSONObject(d).getJSONArray("games");
			for (int g = 0; g < gamesJSONArray.length(); g++) {
				JSONObject jsonGame = gamesJSONArray.getJSONObject(g);
				int gamePk = jsonGame.optInt("gamePk", -1);
				if (gamePk > 0) {
					LOGGER.debug("Adding additional game [" + gamePk + "]");
					games.put(gamePk, jsonGame);
				} else {
					LOGGER.warn("Could not parse 'gamePk': " + jsonGame.toString());
				}
			}

		}
		return games;
	}

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
		List<Game> list = new ArrayList<>();
		Game lastGame = getLastGame(team);
		if (lastGame != null) {
			list.add(lastGame);
		}
		Game currentGame = getCurrentLiveGame(team);
		if (currentGame != null) {
			list.add(currentGame);
		}
		int futureGameIndex = 0;
		while (list.size() < 2) {
			Game futureGame = getFutureGame(team, futureGameIndex++);
			if (futureGame != null) {
				list.add(futureGame);
			} else {
				break;
			}
		}
		return list;
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
	 * Gets a future game for the provided team.
	 * 
	 * @param team
	 *            team to get future game for
	 * @param before
	 *            index index of how many games in the future to get (0 for first game)
	 * @return NHLGame of game in the future for the provided team
	 */
	public Game getFutureGame(Team team, int futureIndex) {
		List<Game> futureGames = games.entrySet().stream()
				.map(Entry::getValue)
				.filter(game -> game.containsTeam(team))
				.filter(game -> !game.getStatus().isStarted())
				.collect(Collectors.toList());
		if (futureIndex >= futureGames.size()) {
			return null;
		}
		return futureGames.get(futureIndex);
	}
	
	/**
	 * <p>
	 * Gets the next game for the provided team.
	 * </p>
	 * <p>
	 * See {@link #getFutureGame(Team, int)}
	 * </p>
	 * 
	 * @param team
	 *            team to get next game for
	 * @return NHLGame of next game for the provided team
	 */
	public Game getNextGame(Team team) {
		return getFutureGame(team, 0);
	}

	/**
	 * Gets a previous game for the provided team.
	 * 
	 * @param team
	 *            team to get previous game for
	 * @param before
	 *            index index of how many games after to get (0 for first games)
	 * @return NHLGame of next game for the provided team
	 */
	public Game getPastGame(Team team, int beforeIndex) {
		List<Game> previousGames = games.entrySet().stream()
				.map(Entry::getValue)
				.filter(game -> game.containsTeam(team))
				.filter(game -> game.getStatus().isFinished())
				.collect(Collectors.toList());
		if (beforeIndex >= previousGames.size()) {
			return null;
		}
		return previousGames.get(previousGames.size() - 1 - beforeIndex);
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
		return getPastGame(team, 0);
	}

	/**
	 * Gets the current game for the provided team
	 * 
	 * @param team
	 *            team to get current game for
	 * @return
	 */
	public Game getCurrentLiveGame(Team team) {
		return games.entrySet().stream()
				.map(Entry::getValue)
				.filter(game -> game.containsTeam(team))
				.filter(game -> game.getStatus().isLive())
				.findAny()
				.orElse(null);
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
				.filter(game -> GameDayChannel.getChannelName(game).equalsIgnoreCase(channelName))
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
	public GameTracker getGameTracker(Game game) {
		if (activeGameTrackers.containsKey(game)) {
			// NHLGameTracker already exists
			LOGGER.debug("NHLGameTracker exists: " + game);
			return activeGameTrackers.get(game);
		} else {
			LOGGER.debug("NHLGameTracker does not exist: " + game);
			return GameTracker.get(game);
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
				.filter(game -> !getActiveGames(team).contains(game))
				.collect(Collectors.toList());
	}

	public boolean isGameActive(Team team, String channelName) {
		return getActiveGames(team).stream()
				.anyMatch(game -> channelName.equalsIgnoreCase(GameDayChannel.getChannelName(game)));
	}

	Map<Game, GameTracker> getActiveGameTrackers() {
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
		return lastUpdate;
	}

	public Set<Game> getGames() {
		return new HashSet<>(games.values());
	}

	public GameTracker toGameTracker(Game game) {
		return GameTracker.get(game);
	}

	public boolean isGameExist(Game game) {
		return games.containsKey(game.getGamePk());
	}
}
