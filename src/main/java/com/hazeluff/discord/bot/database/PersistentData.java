package com.hazeluff.discord.bot.database;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.database.channel.ChannelMessagesData;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMetaData;
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
	private final ChannelMessagesData channelMessagesData;
	private final GDCMetaData gdcMetaData;


	PersistentData(MongoDatabase database,
			PreferencesData preferencesData,
			ChannelMessagesData channelMessagesData, GDCMetaData gdcMetaData) {
		this.database = database;
		this.preferencesData = preferencesData;
		this.channelMessagesData = channelMessagesData;
		this.gdcMetaData = gdcMetaData;
	}

	public static PersistentData load(String hostName, int port, String userName, String password) {
		return load(getDatabase(hostName, port, userName, password));
	}

	static PersistentData load(MongoDatabase database) {
		PreferencesData preferencesManager = PreferencesData.load(database);
		ChannelMessagesData channelMessagesData = ChannelMessagesData.load(database);
		GDCMetaData gdcMetaData = GDCMetaData.load(database);
		return new PersistentData(database, preferencesManager, channelMessagesData, gdcMetaData);
	}

	@SuppressWarnings("resource")
	private static MongoDatabase getDatabase(String hostName, int port, String userName, String password) {
		MongoClient client;
		if (userName != null && password != null) {
			MongoCredential credentials = MongoCredential.createCredential(userName, Config.MONGO_DATABASE_NAME,
					password.toCharArray());
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

	public ChannelMessagesData getChannelMessagesData() {
		return channelMessagesData;
	}

	public GDCMetaData getGDCMetaData() {
		return gdcMetaData;
	}

	public MongoDatabase getMongoDatabase() {
		return database;
	}

}
