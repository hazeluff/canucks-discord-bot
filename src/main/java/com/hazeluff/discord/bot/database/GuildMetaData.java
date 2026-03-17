package com.hazeluff.discord.bot.database;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class GuildMetaData extends DatabaseManager {

	GuildMetaData(MongoDatabase database) {
		super(database);
	}

	public static GuildMetaData load(MongoDatabase database) {
		return new GuildMetaData(database);
	}

	private MongoCollection<Document> getCollection() {
		return getDatabase().getCollection("guild-meta");
	}

	public void save(GuildMeta guildMeta) {
		guildMeta.saveToCollection(getCollection());
	}

	public GuildMeta loadMeta(long guildId, int season) {
		return GuildMeta.findFromCollection(getCollection(), guildId, season);
	}
}
