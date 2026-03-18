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
	private static final String GAME_ID_KEY = "gameId";
	private static final String PARENT_CHANNEL_ID_KEY = "parentChannelId";
	private static final String INTRO_MESSAGE_ID_KEY = "intro-messageId";
	private static final String SUMMARY_MESSAGE_ID_KEY = "summary-messageId";
	private static final String GOAL_MESSAGE_IDS_KEY = "goal-messageIds";
	private static final String PENALTY_MESSAGE_IDS_KEY = "penalty-messageIds";

	private final long channelId;
	private final long gameId;
	private Long parentChannelId;
	private Long introMessageId;
	private Long summaryMessageId;

	private String strGoalMessages;
	private String strPenaltyMessages;

	GDCMeta(long channelId, long gameId, Long parentChannelId) {
		this.channelId = channelId;
		this.gameId = gameId;
		this.parentChannelId = parentChannelId;
	}

	GDCMeta(
		long channelId,
		long gameId,
		Long parentChannelId,
		Long introMessageId,
		Long summaryMessageId,
		String strGoalMessages,
		String strPenaltyMessages
	) {
		this(channelId, gameId, parentChannelId);
		this.introMessageId = introMessageId;
		this.summaryMessageId = summaryMessageId;
		this.strGoalMessages = strGoalMessages;
		this.strPenaltyMessages = strPenaltyMessages;
	}

	public static GDCMeta forChannel(long channelId, long gameId) {
		return new GDCMeta(channelId, gameId, null);
	}

	public static GDCMeta forThread(long channelId, long gameId, Long parentChannelId) {
		return new GDCMeta(channelId, gameId, parentChannelId);
	}

	static GDCMeta findFromCollection(MongoCollection<Document> collection, Document filter) {
		Document doc = collection.find(filter).first();

		if (doc == null) {
			return null;
		}

		long channelId = doc.getLong(CHANNEL_ID_KEY);
		long gameId = doc.containsKey(GAME_ID_KEY) ? doc.getLong(GAME_ID_KEY) : -1;
		Long parentChannelId = doc.getLong(PARENT_CHANNEL_ID_KEY);
		
		Long introMessageId = doc.getLong(INTRO_MESSAGE_ID_KEY);
		Long summaryMessageId = doc.getLong(SUMMARY_MESSAGE_ID_KEY);
		String goalMessageIds = doc.getString(GOAL_MESSAGE_IDS_KEY);
		String penaltyMessageIds = doc.getString(PENALTY_MESSAGE_IDS_KEY);

		return new GDCMeta(channelId, gameId, parentChannelId, introMessageId, summaryMessageId,
				goalMessageIds, penaltyMessageIds);
	}

	static GDCMeta findByChannelId(MongoCollection<Document> collection, long channelId, long gameId) {
		return findFromCollection(
			collection,
			new Document()
				.append(CHANNEL_ID_KEY, channelId)
				.append(GAME_ID_KEY, gameId)
		);
	}

	static GDCMeta findByParentId(MongoCollection<Document> collection, Long parentChannelId, long gameId) {
		return findFromCollection(
			collection,
			new Document()
				.append(PARENT_CHANNEL_ID_KEY, parentChannelId)
				.append(GAME_ID_KEY, gameId)
		);
	}

	void saveToCollection(MongoCollection<Document> collection) {
		collection.updateOne(
			new Document(CHANNEL_ID_KEY, channelId)
				.append(GAME_ID_KEY, gameId)
				.append(PARENT_CHANNEL_ID_KEY, parentChannelId),
			new Document("$set", new Document()
				.append(INTRO_MESSAGE_ID_KEY, introMessageId)
				.append(SUMMARY_MESSAGE_ID_KEY, summaryMessageId)
				.append(GOAL_MESSAGE_IDS_KEY, strGoalMessages)
				.append(PENALTY_MESSAGE_IDS_KEY, strPenaltyMessages)
			),
			new UpdateOptions().upsert(true)		
		);
	}

	public Long getChannelId() {
		return channelId;
	}

	public Long getParentChannelId() {
		return parentChannelId;
	}

	public Long getIntroMessageId() {
		return introMessageId;
	}

	public void setIntroMessageId(Long messageId) {
		this.introMessageId = messageId;
	}

	public Long getSummaryMessageId() {
		return summaryMessageId;
	}

	public void setSummaryMessageId(Long messageId) {
		this.summaryMessageId = messageId;
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

	/**
	 * Set the message ids with Map<GoalId, MessageId>
	 * 
	 * @param goalMessages
	 */
	public void setGoalMessageIds(Map<Integer, Long> goalMessages) {
		BsonDocument doc = new BsonDocument();
		doc.putAll(goalMessages.entrySet().stream()
				.collect(Collectors.toMap(
						e -> String.valueOf(e.getKey()),
						e -> new BsonInt64(e.getValue())
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
