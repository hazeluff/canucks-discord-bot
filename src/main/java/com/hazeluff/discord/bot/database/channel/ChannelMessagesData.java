package com.hazeluff.discord.bot.database.channel;

import org.bson.Document;

import com.hazeluff.discord.bot.database.DatabaseManager;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class ChannelMessagesData extends DatabaseManager {

	ChannelMessagesData(MongoDatabase database) {
		super(database);
	}

	public static ChannelMessagesData load(MongoDatabase database) {
		return new ChannelMessagesData(database);
	}

	private MongoCollection<Document> getCollection() {
		return getDatabase().getCollection("channel-messages");
	}

	public void saveMessage(ChannelMessage message) {
		message.saveToCollection(getCollection());
	}

	/**
	 * 
	 * @param channelId
	 * @param messageKey
	 *            designated id for the message. e.g. "gameday-summary"
	 * @return
	 */
	public ChannelMessage loadMessage(long channelId, String messageKeyId) {
		return ChannelMessage.findFromCollection(getCollection(), channelId, messageKeyId);
	}
}
