package com.hazeluff.discord.bot.database.preferences;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.database.DatabaseManager;
import com.hazeluff.discord.nhl.NHLTeams.Team;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;

/**
 * This class is used to manage preferences of Guilds and Users. Preferences are stored in MongoDB.
 */
public class PreferencesData extends DatabaseManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesData.class);

	// GuildID -> GuildPreferences
	private final Map<Long, GuildPreferences> guildPreferences;

	PreferencesData(MongoDatabase database, Map<Long, GuildPreferences> guildPreferences) {
		super(database);
		this.guildPreferences = guildPreferences;
	}

	public static PreferencesData load(MongoDatabase database) {
		return new PreferencesData(database, loadGuildPreferences(database.getCollection("guilds")));
	}

	private MongoCollection<Document> getCollection() {
		return getCollection(getDatabase());
	}

	private static MongoCollection<Document> getCollection(MongoDatabase database) {
		return database.getCollection("guilds");
	}

	@SuppressWarnings("unchecked")
	static Map<Long, GuildPreferences> loadGuildPreferences(MongoCollection<Document> guildCollection) {
		LOGGER.info("Loading Guild preferences...");
		Map<Long, GuildPreferences> guildPreferences = new ConcurrentHashMap<>();
		MongoCursor<Document> iterator = guildCollection.find().iterator();
		// Load Guild preferences
		while (iterator.hasNext()) {
			Document doc = iterator.next();
			long id = doc.getLong("id");
			Set<Team> teams;

			if (doc.containsKey("teams")) {
				teams = ((List<Integer>) doc.get("teams")).stream().map(Team::parse).collect(Collectors.toSet());
			} else {
				teams = new HashSet<>();
			}

			Long gdcChannelId = null;
			if (doc.containsKey("gdcChannelId")) {
				gdcChannelId = doc.getLong("gdcChannelId");
			}

			GuildPreferences preferences = new GuildPreferences(
				teams,
				gdcChannelId
			);
			guildPreferences.put(id, preferences);
		}

		LOGGER.info("Guild Preferences loaded.");
		return guildPreferences;
	}

	public GuildPreferences getGuildPreferences(long guildId) {
		if (!guildPreferences.containsKey(guildId)) {
			GuildPreferences newPref = new GuildPreferences(new HashSet<>(), null);
			guildPreferences.put(guildId, newPref);
			saveToCollection(getCollection(), guildId, newPref);
		}

		return guildPreferences.get(guildId);
	}

	/**
	 * Updates the guild's subscribed team to the specified one.
	 * 
	 * @param guildId
	 *            id of the guild
	 * @param team
	 *            team to subscribe to
	 */
	public void subscribeGuild(long guildId, Team team) {
		LOGGER.info("Subscribing guild to team. guildId={}, team={}", guildId, team);
		GuildPreferences pref = getPreferences(guildId);

		pref.addTeam(team);

		saveToCollection(getCollection(), guildId, pref);
	}

	/**
	 * Updates the guild to have no subscribed team.
	 * 
	 * @param guildId
	 *            id of the guild
	 * @param team
	 *            team to unsubscribe from. null to unsubscribe from all.
	 */
	public void unsubscribeGuild(long guildId, Team team) {
		LOGGER.info("Unsubscribing guild from team. guildId={} team={}", guildId, team);
		GuildPreferences pref = getPreferences(guildId);

		if (team != null) {
			pref.removeTeam(team);
		}

		saveToCollection(getCollection(), guildId, pref);
	}

	public void setGameDayChannelId(Long guildId, Long channelId) {
		LOGGER.info("Setting GDC Channel Id. guildId={} channelId={}", guildId, channelId);
		GuildPreferences pref = getPreferences(guildId);

		pref.setGameDayChannelId(channelId);

		saveToCollection(getCollection(), guildId, pref);
	}

	private GuildPreferences getPreferences(Long guildId) {
		if (!guildPreferences.containsKey(guildId)) {
			guildPreferences.put(guildId, new GuildPreferences());
		}

		return guildPreferences.get(guildId);
	}
	
	static void saveToCollection(MongoCollection<Document> guildCollection, long guildId, GuildPreferences pref) {
		List<Integer> teamIds = pref.getTeams().stream()
				.map(preferedTeam -> preferedTeam.getId())
				.collect(Collectors.toList());
		Long gdcChannelId = pref.getGameDayChannelId();

		Document prefDoc = new Document()
				.append("teams", teamIds)
				.append("gdcChannelId", gdcChannelId);

		guildCollection.updateOne(
				new Document("id", guildId),
				new Document("$set", prefDoc), 
				new UpdateOptions().upsert(true));
	}

	Map<Long, GuildPreferences> getGuildPreferences() {
		return guildPreferences;
	}
}
