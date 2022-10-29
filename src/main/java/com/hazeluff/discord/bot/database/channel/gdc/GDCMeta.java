package com.hazeluff.discord.bot.database.channel.gdc;

import java.util.Collections;
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
	private static final String VOTE_MESSAGE_ID_KEY = "vote-messageId";
	private static final String SUMMARY_MESSAGE_ID_KEY = "summary-messageId";
	private static final String GOAL_MESSAGE_IDS_KEY = "goal-messageIds";
	private static final String PENALTY_MESSAGE_IDS_KEY = "penalty-messageIds";

	private final long channelId;
	private Long summaryMessageId;
	private Long voteMessageId;

	private String strGoalMessages;
	private String strPenaltyMessages;

	GDCMeta(long channelId) {
		this.channelId = channelId;
	}

	GDCMeta(long channelId, Long summaryMessageId, Long voteMessageId, String strGoalMessages,
			String strPenaltyMessages) {
		this(channelId);
		this.summaryMessageId = summaryMessageId;
		this.voteMessageId = voteMessageId;
		this.strGoalMessages = strGoalMessages;
		this.strPenaltyMessages = strPenaltyMessages;
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
		Long voteMessageId = doc.containsKey(VOTE_MESSAGE_ID_KEY) ? doc.getLong(VOTE_MESSAGE_ID_KEY) : null;
		String goalMessageIds = doc.getString(GOAL_MESSAGE_IDS_KEY);
		String penaltyMessageIds = doc.getString(PENALTY_MESSAGE_IDS_KEY);

		return new GDCMeta(channelId, summaryMessageId, voteMessageId, goalMessageIds, penaltyMessageIds);
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
						.append(SUMMARY_MESSAGE_ID_KEY, summaryMessageId)
						.append(VOTE_MESSAGE_ID_KEY, voteMessageId)
						.append(GOAL_MESSAGE_IDS_KEY, strGoalMessages)
						.append(PENALTY_MESSAGE_IDS_KEY, strPenaltyMessages)
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

	public Long getVoteMessageId() {
		return voteMessageId;
	}

	public void setVoteMessageId(Long messageId) {
		this.voteMessageId = messageId;
	}

	public Map<Integer, Long> getGoalMessageIds() {
		if (strGoalMessages == null) {
			return Collections.emptyMap();
		}
		return BsonDocument.parse(strGoalMessages)
				.entrySet()
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

	public Map<Integer, Long> getPenaltyMessageIds() {
		if (strPenaltyMessages == null) {
			return Collections.emptyMap();
		}
		return BsonDocument.parse(strPenaltyMessages).entrySet().stream()
				.collect(Collectors.toMap(
						e -> Integer.parseInt(e.getKey()), 
						e -> e.getValue().asInt64().getValue()));
	}

	public void setPenaltyMessageIds(Map<Integer, Message> penaltyMessages) {
		BsonDocument doc = new BsonDocument();
		doc.putAll(penaltyMessages.entrySet().stream().collect(
				Collectors.toMap(
						e -> String.valueOf(e.getKey()), 
						e -> new BsonInt64(e.getValue().getId().asLong()))));
		strPenaltyMessages = doc.toJson();
	}
}
