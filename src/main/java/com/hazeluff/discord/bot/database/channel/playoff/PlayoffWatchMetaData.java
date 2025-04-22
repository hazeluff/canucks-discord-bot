package com.hazeluff.discord.bot.database.channel.playoff;

import org.bson.Document;

import com.hazeluff.discord.bot.database.DatabaseManager;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class PlayoffWatchMetaData extends DatabaseManager {

	PlayoffWatchMetaData(MongoDatabase database) {
		super(database);
	}

	public static PlayoffWatchMetaData load(MongoDatabase database) {
		return new PlayoffWatchMetaData(database);
	}

	private MongoCollection<Document> getCollection() {
		return getDatabase().getCollection("playoff-watch-meta");
	}

	public void save(PlayoffWatchMeta playoffMeta) {
		playoffMeta.saveToCollection(getCollection());
	}

	/**
	 * 
	 * @param channelId
	 * @param messageKey
	 *            designated id for the message. e.g. "gameday-summary"
	 * @return
	 */
	public PlayoffWatchMeta loadMeta(long channelId) {
		return PlayoffWatchMeta.findFromCollection(getCollection(), channelId);
	}
}
