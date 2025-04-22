package com.hazeluff.discord.bot.discord;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.spec.CategoryCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.core.spec.TextChannelCreateSpec;
import discord4j.core.spec.TextChannelEditSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Provides methods that interface with Discord. The methods provide error handling.
 */
public class DiscordManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(DiscordManager.class);

	private final GatewayDiscordClient client;
	private Snowflake id;
	private Long applicationId;

	public DiscordManager(GatewayDiscordClient client) {
		this.client = client;
	}

	/*
	 * Non Static Methods
	 */
	public GatewayDiscordClient getClient() {
		return client;
	}

	public Snowflake getId() {
		if (id == null) {
			id = getClient().getSelfId();
		}
		return id;
	}

	public Long getApplicationId() {
		if (applicationId == null) {
			applicationId = block(getClient().getRestClient().getApplicationId());
		}
		return applicationId;
	}

	/**
	 * Determines if the user of the DiscordClient is the author of the specified
	 * message.
	 * 
	 * @param message
	 *            message to determine if client's user is the author of
	 * @return true, if message is authored by client's user.<br>
	 *         false, otherwise.
	 */
	public boolean isAuthorOfMessage(Message message) {
		if (message == null) {
			logNullArgumentsStackTrace("`message` was null.");
			return false;
		}
		return message.getAuthor().map(User::getId).map(getId()::equals).orElse(false);
	}

	public User getUser(long userId) {
		return getClient().getUserById(Snowflake.of(userId)).retry(0).timeout(Duration.ofMillis(500))
				.onErrorResume(DiscordManager::handleError).blockOptional().orElseGet(() -> null);
	}

	public void changePresence(ClientPresence presence) {
		subscribe(getClient().updatePresence(presence));
	}

	public List<Guild> getGuilds() {
		return block(getClient().getGuilds());
	}

	public Message getMessage(long channelId, long messageId) {
		return block(getClient().getMessageById(Snowflake.of(channelId), Snowflake.of(messageId)));
	}

	/*
	 * Static Methods
	 */
	public static Message sendAndGetMessage(TextChannel channel, MessageCreateSpec messageSpec) {
		if (channel == null) {
			logNullArgumentsStackTrace("`channel` was null.");
			return null;
		}

		if (messageSpec == null) {
			logNullArgumentsStackTrace("`messageSpec` was null.");
			return null;
		}
		return block(channel.createMessage(messageSpec));
	}

	public static Message sendAndGetMessage(TextChannel channel, String message) {
		if (message == null) {
			logNullArgumentsStackTrace("`message` was null.");
			return null;
		}

		MessageCreateSpec messageCreateSpec = MessageCreateSpec.builder().content(message).build();
		return sendAndGetMessage(channel, messageCreateSpec);
	}

	public static void sendMessage(TextChannel channel, MessageCreateSpec messageSpec) {
		if (channel == null) {
			logNullArgumentsStackTrace("`channel` was null.");
			return;
		}

		if (messageSpec == null) {
			logNullArgumentsStackTrace("`messageSpec` was null.");
			return;
		}
		subscribe(channel.createMessage(messageSpec));
	}

	public static void sendMessage(TextChannel channel, String message) {
		if (channel == null) {
			logNullArgumentsStackTrace("`channel` was null.");
			return;
		}

		if (message == null) {
			logNullArgumentsStackTrace("`message` was null.");
			return;
		}

		MessageCreateSpec messageCreateSpec = MessageCreateSpec.builder().content(message).build();
		sendMessage(channel, messageCreateSpec);
	}

	/**
	 * Updates the message in Discord. Returns the new Message if successful. Else
	 * it returns the original Message.
	 * 
	 * @param messages
	 *            existing message in Discord
	 * @param newMessage
	 *            new message
	 * @return
	 */
	public static Message updateAndGetMessage(Message message, String newMessage) {
		if (message == null) {
			logNullArgumentsStackTrace("`message` was null.");
			return null;
		}

		if (newMessage == null) {
			logNullArgumentsStackTrace("`newMessage` was null.");
			return null;
		}

		MessageEditSpec messageEditSpec = MessageEditSpec.builder().contentOrNull(newMessage).build();
		return block(message.edit(messageEditSpec).onErrorReturn(null));
	}

	/**
	 * Updates the message in Discord. Returns the new Message if successful. Else
	 * it returns the original Message.
	 * 
	 * @param messages
	 *            existing message in Discord
	 * @param newMessage
	 *            new message
	 * @return
	 */
	public static void updateMessage(Message message, String newMessage) {
		if (message == null) {
			logNullArgumentsStackTrace("`message` was null.");
			return;
		}

		if (newMessage == null) {
			logNullArgumentsStackTrace("`newMessage` was null.");
			return;
		}

		MessageEditSpec messageEditSpec = MessageEditSpec.builder().contentOrNull(newMessage).build();
		subscribe(message.edit(messageEditSpec));
	}

	/**
	 * Updates the message in Discord. Returns the new Message if successful. Else
	 * it returns the original Message.
	 * 
	 * @param messages
	 *            existing message in Discord
	 * @param newMessage
	 *            new message
	 * @return
	 */
	public static void updateMessage(Message message, MessageEditSpec newMessageSpec) {
		if (message == null) {
			logNullArgumentsStackTrace("`message` was null.");
			return;
		}

		if (newMessageSpec == null) {
			logNullArgumentsStackTrace("`newMessageSpec` was null.");
			return;
		}

		subscribe(message.edit(newMessageSpec));
	}

	/**
	 * Deletes the specified message in Discord
	 * 
	 * @param message
	 *            message to delete in Discord
	 */
	public static void deleteMessage(Message message) {
		if (message == null) {
			logNullArgumentsStackTrace("`message` was null.");
			return;
		}

		subscribe(message.delete());
	}

	/**
	 * Gets a list of pinned messages in the specified channel.
	 * 
	 * @param channel
	 *            channel to get messages from
	 * @return List<Message> of messages in the channel
	 */
	public static List<Message> getPinnedMessages(TextChannel channel) {
		if (channel == null) {
			logNullArgumentsStackTrace("`channel` was null.");
			return null;
		}

		return block(channel.getPinnedMessages());
	}

	/**
	 * Deletes the specified channel
	 * 
	 * @param channel
	 *            channel to delete
	 */
	public static void deleteChannel(TextChannel channel) {
		if (channel == null) {
			logNullArgumentsStackTrace("`channel` was null.");
			return;
		}

		subscribe(channel.delete());
	}

	/**
	 * Creates channel in specified guild
	 * 
	 * @param guild
	 *            guild to create the channel in
	 * @param channelName
	 *            name of channel to create
	 * @return TextChannel that was created
	 */
	public static TextChannel createAndGetChannel(Guild guild, String channelName) {
		TextChannelCreateSpec textChannelCreateSpec = TextChannelCreateSpec.builder().name(channelName).build();
		return createAndGetChannel(guild, textChannelCreateSpec);
	}

	/**
	 * Creates channel in specified guild
	 * 
	 * @param guild
	 *            guild to create the channel in
	 * @param channelName
	 *            name of channel to create
	 * @return TextChannel that was created
	 */
	public static TextChannel createAndGetChannel(Guild guild, TextChannelCreateSpec channelSpec) {
		if (guild == null) {
			logNullArgumentsStackTrace("`guild` was null.");
			return null;
		}

		if (channelSpec == null) {
			logNullArgumentsStackTrace("`channelSpec` was null.");
			return null;
		}

		return block(guild.createTextChannel(channelSpec).onErrorReturn(null));
	}

	public static TextChannel getTextChannel(Guild guild, String channelName) {

		if (guild == null) {
			logNullArgumentsStackTrace("`guild` was null.");
			return null;
		}

		if (channelName == null) {
			logNullArgumentsStackTrace("`spec` was null.");
			return null;
		}

		return block(guild.getChannels()
				.filter(TextChannel.class::isInstance)
				.filter(txtchnl -> txtchnl.getName().equals(channelName))
				.take(1)
				.cast(TextChannel.class)
				.next()
				.onErrorReturn(null)
		);
	}

	public static TextChannel getOrCreateTextChannel(Guild guild, String channelName) {
		TextChannel channel = getTextChannel(guild, channelName);
		return channel != null ? channel : createAndGetChannel(guild, channelName);
	}

	/**
	 * Pins the message to the specified channels
	 * 
	 * @param message
	 *            existing message in Discord
	 */
	public static void pinMessage(Message message) {
		if (message == null) {
			logNullArgumentsStackTrace("`message` was null.");
			return;
		}

		subscribe(message.pin());
	}

	/**
	 * Pins the message to the specified channels
	 * 
	 * @param message
	 *            existing message in Discord
	 */
	public static void unpinMessage(Message message) {
		if (message == null) {
			logNullArgumentsStackTrace("`message` was null.");
			return;
		}

		subscribe(message.unpin());
	}

	/**
	 * Creates a category with the given name.
	 * 
	 * @param guild
	 *            guild to create the category in
	 * @param categoryName
	 *            name of the category
	 */
	public static Category createCategory(Guild guild, String categoryName) {
		if (guild == null) {
			logNullArgumentsStackTrace("`guild` was null.");
			return null;
		}

		if (categoryName == null) {
			logNullArgumentsStackTrace("`categoryName` was null.");
			return null;
		}

		CategoryCreateSpec categoryCreateSpec = CategoryCreateSpec.builder().name(categoryName).build();
		return guild.createCategory(categoryCreateSpec).block();
	}

	/**
	 * Gets the category with the given name.
	 * 
	 * @param guild
	 *            guild to get the category from
	 * @param categoryName
	 *            name of the category
	 * 
	 */
	public static Category getCategory(Guild guild, String categoryName) {
		if (guild == null) {
			logNullArgumentsStackTrace("`guild` was null.");
			return null;
		}

		if (categoryName == null) {
			logNullArgumentsStackTrace("`categoryName` was null.");
			return null;
		}

		return block(guild.getChannels()
				.filter(channel -> (channel instanceof Category))
				.filter(category -> category.getName().equalsIgnoreCase(categoryName))
				.take(1)
				.cast(Category.class)
				.next()
				.onErrorReturn(null)
		);
	}

	public static Category getOrCreateCategory(Guild guild, String categoryName) {
		Category category = getCategory(guild, categoryName);
		return category != null ? category : createCategory(guild, categoryName);
	}

	/**
	 * Moves the given channel into the given category.
	 * 
	 * @param category
	 *            category to move channel into
	 * @param channel
	 *            channel to move
	 */
	public static void moveChannel(Category category, TextChannel channel) {
		if (category == null) {
			logNullArgumentsStackTrace("`category` was null.");
			return;
		}

		if (channel == null) {
			logNullArgumentsStackTrace("`channel` was null.");
			return;
		}
		LOGGER.debug("Moving channel into category. channel={}, category={}", channel.getName(), category.getName());

		TextChannelEditSpec textChannelEditSpec = TextChannelEditSpec.builder().parentIdOrNull(category.getId())
				.build();
		subscribe(channel.edit(textChannelEditSpec));
	}

	public static Category getCategory(TextChannel channel) {
		if (channel == null) {
			logNullArgumentsStackTrace("`channel` was null.");
			return null;
		}

		return block(channel.getCategory().onErrorReturn(null));
	}

	public static List<TextChannel> getTextChannels(Guild guild) {
		if (guild == null) {
			logNullArgumentsStackTrace("`guild` was null.");
			return null;
		}

		return block(guild.getChannels()
				.filter(channel -> (channel instanceof TextChannel))
				.cast(TextChannel.class));
	}

	private static void logNullArgumentsStackTrace(String message) {
		if (message == null || message.isEmpty()) {
			message = "One or more argument(s) were null.";
		}
		LOGGER.warn(message, new NullPointerException());
	}

	public static <T> T block(Mono<T> mono) {
		return mono.onErrorResume(DiscordManager::handleError)
				.retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(10)))
				.blockOptional()
				.orElseGet(() -> null);
	}

	public static <T> void subscribe(Mono<T> mono) {
		mono.onErrorResume(DiscordManager::handleError)
				.retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(20)))
				.subscribe();
	}

	public static <T> List<T> block(Flux<T> flux) {
		return flux.onErrorResume(DiscordManager::handleError)
				.retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(10)))
				.collectList()
				.blockOptional()
				.orElseGet(() -> null);
	}

	public static <T> void subscribe(Flux<T> flux) {
		flux.onErrorResume(DiscordManager::handleError)
				.retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(20)))
				.subscribe();
	}

	private static <T> Mono<T> handleError(Throwable t) {
		LOGGER.error("Error occurred.", t);
		return Mono.empty();
	}
}
