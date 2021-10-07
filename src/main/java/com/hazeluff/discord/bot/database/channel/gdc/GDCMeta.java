package com.hazeluff.discord.bot.database.channel.gdc;

import java.util.Map;
import java.util.stream.Collectors;

import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;

import discord4j.core.object.entity.Message;

public class GDCMeta {
	private static final String CHANNEL_ID_KEY = "channelId";
	private static final String POLL_MESSAGE_ID_KEY = "poll-messageId";
	private static final String SUMMARY_MESSAGE_ID_KEY = "summary-messageId";
	private static final String GOAL_MESSAGE_IDS_KEY = "goal-messageIds";

	private final long channelId;
	private long summaryMessageId;
	private long pollMessageId;

	private String strGoalMessages;

	GDCMeta(long channelId) {
		this.channelId = channelId;
	}

	GDCMeta(long channelId, long summaryMessageId, long pollMessageId, String strGoalMessages) {
		this(channelId);
		this.summaryMessageId = summaryMessageId;
		this.pollMessageId = pollMessageId;
		this.strGoalMessages = strGoalMessages;
	}

	public static GDCMeta of(long channelId) {
		return new GDCMeta(channelId);
	}

	static GDCMeta findFromCollection(MongoCollection<Document> collection, Document filter) {
		Document doc = collection.find(filter).first();

		if (doc == null) {
			return null;
		}

		long channelId = doc.getLong(CHANNEL_ID_KEY);
		Long summaryMessageId = doc.getLong(SUMMARY_MESSAGE_ID_KEY);
		Long pollMessageId = doc.getLong(POLL_MESSAGE_ID_KEY);
		String goalMessageIds = doc.getString(GOAL_MESSAGE_IDS_KEY);

		return new GDCMeta(channelId, summaryMessageId, pollMessageId, goalMessageIds);
	}

	static GDCMeta findFromCollection(MongoCollection<Document> collection, long channelId) {
		return findFromCollection(
				collection, 
				new Document().append(CHANNEL_ID_KEY, channelId)
		);
	}

	void saveToCollection(MongoCollection<Document> collection) {
		collection.updateOne(
				new Document(CHANNEL_ID_KEY,
						channelId),
				new Document("$set", new Document()
						.append(SUMMARY_MESSAGE_ID_KEY,
								summaryMessageId)
						.append(POLL_MESSAGE_ID_KEY, pollMessageId)
						.append(GOAL_MESSAGE_IDS_KEY, strGoalMessages)
				),
				new UpdateOptions().upsert(true));
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

	public Long getPollMessageId() {
		return pollMessageId;
	}

	public void setPollMessageId(Long messageId) {
		this.pollMessageId = messageId;
	}

	public Map<Integer, Long> getGoalMessageIds() {
		return 
		BsonDocument.parse(strGoalMessages).entrySet()
				.stream()
				.collect(Collectors.toMap(
						e -> Integer.parseInt(e.getKey()), 
						e -> e.getValue().asInt64().getValue()));
	}

	public void setGoalMessageIds(Map<Integer, Message> goalMessages) {
		BsonDocument doc = new BsonDocument();
		doc.putAll(goalMessages.entrySet().stream()
				.collect(Collectors.toMap(
						e -> String.valueOf(e.getKey()),
						e -> new BsonInt64(e.getValue().getId().asLong())
				))
		);
		strGoalMessages = doc.toJson();
	}

	public void setGoalMessageIds(String strGoalMessages) {
		this.strGoalMessages = strGoalMessages;
	}
}
