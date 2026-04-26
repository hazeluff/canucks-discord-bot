package com.hazeluff.discord.bot.database.channel.playoff;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;

public class PlayoffWatchMeta {
	private static final String CHANNEL_ID_KEY = "channelId";
	private static final String SUMMARY_MESSAGE_ID_KEY = "summary-messageId";
	private static final String SCHEDULE_MESSAGE_ID_KEY = "schedule-messageId";

	private final long channelId;
	private Long summaryMessageId;
	private Long scheduleMessageId;

	PlayoffWatchMeta(long channelId) {
		this.channelId = channelId;
	}

	PlayoffWatchMeta(long channelId, Long summaryMessageId, Long scheduleMessageId) {
		this(channelId);
		this.summaryMessageId = summaryMessageId;
		this.scheduleMessageId = scheduleMessageId;
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
		Long scheduleMessageId = doc.getLong(SCHEDULE_MESSAGE_ID_KEY);

		return new PlayoffWatchMeta(channelId, summaryMessageId, scheduleMessageId);
	}

	static PlayoffWatchMeta findFromCollection(MongoCollection<Document> collection, long channelId) {
		return findFromCollection(collection,
			new Document().append(CHANNEL_ID_KEY, channelId)
		);
	}

	void saveToCollection(MongoCollection<Document> collection) {
		collection.updateOne(
			new Document(CHANNEL_ID_KEY, channelId),
			new Document("$set", new Document()
				.append(SUMMARY_MESSAGE_ID_KEY, summaryMessageId)
				.append(SCHEDULE_MESSAGE_ID_KEY, scheduleMessageId)
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

	public Long getScheduleMessageId() {
		return scheduleMessageId;
	}

	public void setScheduleMessageId(Long messageId) {
		this.scheduleMessageId = messageId;
	}
}
