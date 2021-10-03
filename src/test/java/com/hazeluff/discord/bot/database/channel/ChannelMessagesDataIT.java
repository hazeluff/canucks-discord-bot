package com.hazeluff.discord.bot.database.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.test.DatabaseIT;
import com.mongodb.MongoClient;

public class ChannelMessagesDataIT extends DatabaseIT {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChannelMessagesDataIT.class);

	ChannelMessagesData channelMessagesData;

	private static MongoClient client;

	@Override
	public MongoClient getClient() {
		return client;
	}

	@BeforeClass
	public static void setupConnection() {
		client = createConnection();
	}

	@AfterClass
	public static void closeConnection() {
		closeConnection(client);
	}

	@Before
	public void before() {
		super.before();
		channelMessagesData = new ChannelMessagesData(getDatabase());
	}

	@Test
	public void channelMessagesCanBeSavedAndLoaded() {
		LOGGER.info("polesCanBeSavedAndLoaded");
		long channelId = 100;
		long messageId = 200;
		String messageKeyId = "test";
		ChannelMessage message = new ChannelMessage(channelId, messageId, messageKeyId);
		
		assertNull(channelMessagesData.loadMessage(channelId, messageKeyId));
		channelMessagesData.saveMessage(message);
		assertEquals(message, channelMessagesData.loadMessage(channelId, messageKeyId));
		assertEquals(message, new ChannelMessagesData(getDatabase()).loadMessage(channelId, messageKeyId));
	}
}