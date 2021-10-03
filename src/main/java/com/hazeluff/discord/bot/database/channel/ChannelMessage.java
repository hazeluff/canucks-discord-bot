package com.hazeluff.discord.bot.database.channel;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;

public class ChannelMessage {
	private static final String MESSAGE_KEY_ID_KEY = "messageKeyId";
	private static final String CHANNEL_ID_KEY = "channelId";
	private static final String MESSAGE_ID_KEY = "messageId";

	private final long channelId;
	private final long messageId;
	private final String messageKeyId;

	ChannelMessage(long channelId, long messageId, String messageKeyId) {
		this.channelId = channelId;
		this.messageId = messageId;
		this.messageKeyId = messageKeyId;
	}

	public static ChannelMessage of(long channelId, long messageId, String messageKeyId) {
		return new ChannelMessage(channelId, messageId, messageKeyId);
	}

	static ChannelMessage findFromCollection(MongoCollection<Document> collection, Document filter) {
		Document doc = collection.find(filter).first();

		if (doc == null) {
			return null;
		}

		long messageId = doc.getLong(MESSAGE_ID_KEY);
		long channelId = doc.getLong(CHANNEL_ID_KEY);
		String messageKeyId = doc.getString(MESSAGE_KEY_ID_KEY);

		return new ChannelMessage(channelId, messageId, messageKeyId);
	}

	static ChannelMessage findFromCollection(MongoCollection<Document> collection, long messageId) {
		return findFromCollection(
				collection, 
				new Document()
						.append(MESSAGE_ID_KEY, messageId));
	}

	static ChannelMessage findFromCollection(MongoCollection<Document> collection, long channelId, String messageKeyId) {
		return findFromCollection(
				collection, 
				new Document()
						.append(CHANNEL_ID_KEY, channelId)
						.append(MESSAGE_KEY_ID_KEY, messageKeyId));
	}

	void saveToCollection(MongoCollection<Document> collection) {
		collection.updateOne(
				new Document(MESSAGE_ID_KEY, messageId),
				new Document("$set", new Document()
						.append(CHANNEL_ID_KEY, channelId)
						.append(MESSAGE_KEY_ID_KEY, messageKeyId)),
				new UpdateOptions().upsert(true));
	}

	public long getChannelId() {
		return channelId;
	}

	public long getMessageId() {
		return messageId;
	}

	public String getMessageKeyId() {
		return messageKeyId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (channelId ^ (channelId >>> 32));
		result = prime * result + (int) (messageId ^ (messageId >>> 32));
		result = prime * result + ((messageKeyId == null) ? 0 : messageKeyId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChannelMessage other = (ChannelMessage) obj;
		if (channelId != other.channelId)
			return false;
		if (messageId != other.messageId)
			return false;
		if (messageKeyId == null) {
			if (other.messageKeyId != null)
				return false;
		} else if (!messageKeyId.equals(other.messageKeyId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ChannelMessage [channelId=" + channelId + ", messageId=" + messageId + ", messageKeyId=" + messageKeyId
				+ "]";
	}
}
