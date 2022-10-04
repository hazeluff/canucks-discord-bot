package com.hazeluff.discord.bot.database;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.database.channel.ChannelMessagesData;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMetaData;
import com.hazeluff.discord.bot.database.fuck.FucksData;
import com.hazeluff.discord.bot.database.preferences.PreferencesData;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

/**
 * This class is used to manage preferences of Guilds and Users. Preferences are stored in MongoDB.
 */
public class PersistentData {
	private final MongoDatabase database;

	private final PreferencesData preferencesData;
	private final FucksData fucksData;
	private final ChannelMessagesData channelMessagesData;
	private final GDCMetaData gdcMetaData;


	PersistentData(MongoDatabase database, PreferencesData preferencesData, FucksData fucksData,
			ChannelMessagesData channelMessagesData, GDCMetaData gdcMetaData) {
		this.database = database;
		this.preferencesData = preferencesData;
		this.fucksData = fucksData;
		this.channelMessagesData = channelMessagesData;
		this.gdcMetaData = gdcMetaData;
	}

	public static PersistentData load(String hostName, int port) {
		return load(getDatabase(hostName, port));
	}

	static PersistentData load(MongoDatabase database) {
		PreferencesData preferencesManager = PreferencesData.load(database);
		FucksData fucksManager = FucksData.load(database);
		ChannelMessagesData channelMessagesData = ChannelMessagesData.load(database);
		GDCMetaData gdcMetaData = GDCMetaData.load(database);
		return new PersistentData(database, preferencesManager, fucksManager, channelMessagesData, gdcMetaData);
	}

	@SuppressWarnings("resource")
	private static MongoDatabase getDatabase(String hostName, int port) {
		return new MongoClient(hostName, port)
				.getDatabase(Config.MONGO_DATABASE_NAME);
	}

	public PreferencesData getPreferencesData() {
		return preferencesData;
	}

	public FucksData getFucksData() {
		return fucksData;
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
