package com.hazeluff.discord.bot.database;

import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;

public class GuildMeta {
	// Identifiers
	private static final String GUILD_ID_KEY = "guildId";
	private static final String SEASON_KEY = "season";
	// Data
	private static final String GDC_CHANNELS_KEY = "gdcChannelIds";

	private final long guildId;
	private final int season;
	private final List<Long> gdcChannelIds;

	GuildMeta(long guildId, int season, List<Long> channelIds) {
		this.guildId = guildId;
		this.season = season;
		this.gdcChannelIds = channelIds;
	}

	public static GuildMeta of(long guildId, int season, List<Long> channelIds) {
		return new GuildMeta(guildId, season, channelIds);
	}

	static GuildMeta findFromCollection(MongoCollection<Document> collection, Document filter) {
		Document doc = collection.find(filter).first();

		if (doc == null) {
			return null;
		}

		long guildId = doc.getLong(GUILD_ID_KEY);
		int season = doc.getInteger(SEASON_KEY);
		@SuppressWarnings("unchecked")
		List<Long> channelIds = ((List<Long>) doc.get(GDC_CHANNELS_KEY));

		return new GuildMeta(guildId, season, channelIds);
	}

	static GuildMeta findFromCollection(MongoCollection<Document> collection, long guildId, int season) {
		return findFromCollection(
				collection, 
				new Document()
						.append(GUILD_ID_KEY, guildId)
						.append(SEASON_KEY, season));
	}

	void saveToCollection(MongoCollection<Document> collection) {
		collection.updateOne(
				new Document(GUILD_ID_KEY, guildId)
					.append(SEASON_KEY, season),
				new Document("$set", new Document()
						.append(GDC_CHANNELS_KEY, gdcChannelIds)),
				new UpdateOptions().upsert(true));
	}
}
