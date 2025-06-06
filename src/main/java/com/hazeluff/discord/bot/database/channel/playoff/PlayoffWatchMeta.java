package com.hazeluff.discord.bot.database.channel.playoff;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;

public class PlayoffWatchMeta {
	private static final String CHANNEL_ID_KEY = "channelId";
	private static final String SUMMARY_MESSAGE_ID_KEY = "summary-messageId";

	private final long channelId;
	private Long summaryMessageId;

	PlayoffWatchMeta(long channelId) {
		this.channelId = channelId;
	}

	PlayoffWatchMeta(long channelId,
			Long summaryMessageId) {
		this(channelId);
		this.summaryMessageId = summaryMessageId;
	}

	public static PlayoffWatchMeta of(long channelId) {
		return new PlayoffWatchMeta(channelId);
	}

	static PlayoffWatchMeta findFromCollection(MongoCollection<Document> collection, Document filter) {
		Document doc = collection.find(filter).first();

		if (doc == null) {
			return null;
		}

		long channelId = doc.getLong(CHANNEL_ID_KEY);

		Long summaryMessageId = doc.getLong(SUMMARY_MESSAGE_ID_KEY);

		return new PlayoffWatchMeta(channelId, summaryMessageId);
	}

	static PlayoffWatchMeta findFromCollection(MongoCollection<Document> collection, long channelId) {
		return findFromCollection(
				collection,
			new Document()
				.append(CHANNEL_ID_KEY, channelId)
		);
	}

	void saveToCollection(MongoCollection<Document> collection) {
		collection.updateOne(
			new Document(CHANNEL_ID_KEY, channelId),
			new Document("$set", new Document()
				.append(SUMMARY_MESSAGE_ID_KEY, summaryMessageId)
			),
			new UpdateOptions().upsert(true)
		);
	}

	public Long getChannelId() {
		return channelId;
	}

	public Long getSummaryMessageId() {
		return summaryMessageId;
	}

	public void setSummaryMessageId(Long messageId) {
		this.summaryMessageId = messageId;
	}
}
