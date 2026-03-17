package com.hazeluff.discord.bot.database;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMetaData;
import com.hazeluff.discord.bot.database.channel.playoff.PlayoffWatchMetaData;
import com.hazeluff.discord.bot.database.preferences.PreferencesData;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

/**
 * This class is used to manage preferences of Guilds and Users. Preferences are stored in MongoDB.
 */
public class PersistentData {
	private final MongoDatabase database;

	private final PreferencesData preferencesData;
	private final GDCMetaData gdcMetaData;
	private final PlayoffWatchMetaData playoffWatchMetaData;


	PersistentData(MongoDatabase database, PreferencesData preferencesData,
			GDCMetaData gdcMetaData,
			PlayoffWatchMetaData playoffWatchMetaData) {
		this.database = database;
		this.preferencesData = preferencesData;
		this.gdcMetaData = gdcMetaData;
		this.playoffWatchMetaData = playoffWatchMetaData;
	}

	public static PersistentData load(String hostName, int port, String userName, String password) {
		return load(getDatabase(hostName, port, userName, password));
	}

	static PersistentData load(MongoDatabase database) {
		PreferencesData preferencesManager = PreferencesData.load(database);
		GDCMetaData gdcMetaData = GDCMetaData.load(database);
		PlayoffWatchMetaData playoffWatchMetaData = PlayoffWatchMetaData.load(database);
		return new PersistentData(database, preferencesManager, gdcMetaData, playoffWatchMetaData);
	}

	@SuppressWarnings("resource")
	private static MongoDatabase getDatabase(String hostName, int port, String userName, String password) {
		MongoClient client;
		if (userName != null && password != null) {
			MongoCredential credentials = MongoCredential.createCredential(
					userName, Config.MONGO_DATABASE_NAME, password.toCharArray());
			ServerAddress serverAddress = new ServerAddress(hostName, port);
			client = new MongoClient(serverAddress, credentials, MongoClientOptions.builder().build());
		} else {
			client = new MongoClient(hostName, port);
		}
		return client.getDatabase(Config.MONGO_DATABASE_NAME);
	}

	public PreferencesData getPreferencesData() {
		return preferencesData;
	}

	public GDCMetaData getGDCMetaData() {
		return gdcMetaData;
	}

	public PlayoffWatchMetaData getPlayoffWatchMetaData() {
		return playoffWatchMetaData;
	}

	public MongoDatabase getMongoDatabase() {
		return database;
	}

}
