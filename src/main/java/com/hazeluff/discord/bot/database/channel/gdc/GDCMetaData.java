package com.hazeluff.discord.bot.database.channel.gdc;

import org.bson.Document;

import com.hazeluff.discord.bot.database.DatabaseManager;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class GDCMetaData extends DatabaseManager {

	GDCMetaData(MongoDatabase database) {
		super(database);
	}

	public static GDCMetaData load(MongoDatabase database) {
		return new GDCMetaData(database);
	}

	private MongoCollection<Document> getCollection() {
		return getDatabase().getCollection("gdc-meta");
	}

	public void save(GDCMeta gdcMeta) {
		gdcMeta.saveToCollection(getCollection());
	}

	/**
	 * 
	 * @param channelId
	 * @param messageKey
	 *            designated id for the message. e.g. "gameday-summary"
	 * @return
	 */
	public GDCMeta loadMeta(long channelId, long gameId) {
		return GDCMeta.findFromCollection(getCollection(), channelId, gameId);
	}
}
