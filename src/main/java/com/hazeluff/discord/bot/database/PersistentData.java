package com.hazeluff.discord.bot.database;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.database.channel.ChannelMessagesData;
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


	PersistentData(MongoDatabase database, PreferencesData preferencesData, FucksData fucksData,
			ChannelMessagesData channelMessagesData) {
		this.database = database;
		this.preferencesData = preferencesData;
		this.fucksData = fucksData;
		this.channelMessagesData = channelMessagesData;
	}

	public static PersistentData load() {
		return load(getDatabase());
	}

	static PersistentData load(MongoDatabase database) {
		PreferencesData preferencesManager = PreferencesData.load(database);
		FucksData fucksManager = FucksData.load(database);
		ChannelMessagesData channelMessagesData = ChannelMessagesData.load(database);
		return new PersistentData(database, preferencesManager, fucksManager, channelMessagesData);
	}

	@SuppressWarnings("resource")
	private static MongoDatabase getDatabase() {
		return new MongoClient(Config.MONGO_HOST, Config.MONGO_PORT)
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

	public MongoDatabase getMongoDatabase() {
		return database;
	}

}
