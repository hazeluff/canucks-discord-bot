package com.hazeluff.test;

import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Answers;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

/*
 * Note: We are not using @RunWith(PowerMockRunner.class) as it causes an ExceptionInInitializationError with
 * MongoClient. DiscordClient will not be mocked and will be null. Methods in GuildsPreferencesManager should not both
 * use DiscordClient and MongoDatabase, so that we can test them.
 */
public abstract class DatabaseIT {

	private MongoDatabase mongoDatabase;
	private NHLBot nhlBot;

	public abstract MongoClient getClient();

	public static MongoClient createConnection() {
		return new MongoClient(Config.getMongoHost(), Config.getMongoPort());
	}

	public static void closeConnection(MongoClient client) {
		if (client != null) {
			client.close();
		}
	}

	@BeforeAll
	public void before() {
		mongoDatabase = getClient().getDatabase(Config.MONGO_TEST_DATABASE_NAME);
		nhlBot = mock(NHLBot.class, Answers.RETURNS_DEEP_STUBS);
	}

	@AfterAll
	public void after() {
		mongoDatabase.drop();
	}

	protected MongoDatabase getDatabase() {
		return mongoDatabase;
	}

	protected NHLBot getNHLBot() {
		return nhlBot;
	}
}
