package com.hazeluff.discord.bot;

import static com.hazeluff.discord.utils.Utils.not;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.channel.GDCCategoryManager;
import com.hazeluff.discord.bot.channel.WordcloudChannelManager;
import com.hazeluff.discord.bot.command.Command;
import com.hazeluff.discord.bot.database.PersistentData;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.bot.gdc.GameDayChannelsManager;
import com.hazeluff.discord.bot.listener.MessageListener;
import com.hazeluff.discord.bot.listener.ReactionListener;
import com.hazeluff.discord.nhl.GameScheduler;
import com.hazeluff.discord.utils.Utils;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.request.RouteMatcher;
import discord4j.rest.response.ResponseFunction;
import discord4j.rest.route.Routes;
import reactor.core.publisher.Mono;

public class NHLBot extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(NHLBot.class);

	private static final ClientPresence STARTING_UP_PRESENCE = ClientPresence.doNotDisturb(
			ClientActivity.watching("itself starting up..."));
	private static final ClientPresence ONLINE_PRESENCE = ClientPresence.online(
			ClientActivity.streaming(Config.STATUS_MESSAGE, Config.GIT_URL));
	private static long UPDATE_PLAY_STATUS_INTERVAL = 3600000l;

	private AtomicReference<DiscordManager> discordManager = new AtomicReference<>();
	private PersistentData persistantData;
	private GameScheduler gameScheduler;
	private GameDayChannelsManager gameDayChannelsManager;

	private final MessageListener messageListener = new MessageListener(this);
	private final ReactionListener reactionListener = new ReactionListener(this);

	private final GDCCategoryManager gdcCategoryManager = new GDCCategoryManager(this);
	private final WordcloudChannelManager wcChannelManager = new WordcloudChannelManager(this, gdcCategoryManager);

	private NHLBot() {
		persistantData = null;
		gameScheduler = null;
		gameDayChannelsManager = null;
	}

	/**
	 * 
	 * @param gameScheduler
	 *            null - to run only scripts
	 * @param botToken
	 * @return
	 */
	public static NHLBot create(GameScheduler gameScheduler, String botToken) {
		LOGGER.info("Creating " + Config.APPLICATION_NAME + " v" + Config.VERSION);
		Thread.currentThread().setName(Config.APPLICATION_NAME);

		NHLBot nhlBot = new NHLBot();
		nhlBot.gameScheduler = gameScheduler;

		// Init Discord Client
		nhlBot.initDiscord(botToken);

		while (nhlBot.getDiscordManager() == null) {
			LOGGER.info("Waiting for Discord client to be ready.");
			Utils.sleep(5000);
		}

		LOGGER.info(
				"NHLBot connected and ready."
				+ " id=" + nhlBot.getDiscordManager().getId() + ","
				+ " appId=" + nhlBot.getDiscordManager().getApplicationId());
		return nhlBot;
	}

	@Override
	public void run() {
		LOGGER.info("NHLBot is being intialized.");
		// Set starting up status
		getDiscordManager().changePresence(STARTING_UP_PRESENCE);

		// Init MongoClient/GuildPreferences
		initPersistentData();

		// Attach Listeners
		attachListeners(this);

		// Attach Listeners for Bot Slash Commands
		attachSlashCommandListeners(this);

		/*
		 * Setup Categories and Channels
		 */
		initCategoriesAndChannels();

		LOGGER.info("NHLBot completed initialization.");
		getDiscordManager().changePresence(ONLINE_PRESENCE);

		while (!isInterrupted()) {
			Utils.sleep(UPDATE_PLAY_STATUS_INTERVAL);
			getDiscordManager().changePresence(ONLINE_PRESENCE);
		}
	}

	void initCategoriesAndChannels() {
		LOGGER.info("Setup discord entities (Categories + Channels).");
		List<Guild> guilds = getDiscordManager().getGuilds();

		// Init Static Entities (They must be init in order!!!)
		gdcCategoryManager.init(guilds);
		wcChannelManager.init(guilds);

		// Start the Game Day Channels Manager
		initGameDayChannelsManager();

		// Manage WelcomeChannels (Only for my dev servers)
		updateWelcomeChannel();
	}

	void initPersistentData() {
		LOGGER.info("Initializing Persistent Data.");
		this.persistantData = PersistentData.load(Config.getMongoHost(), Config.MONGO_PORT);
	}

	void initGameDayChannelsManager() {
		if (Config.Debug.isLoadGames()) {
			LOGGER.info("Initializing GameDayChannelsManager.");
			this.gameDayChannelsManager = new GameDayChannelsManager(this);
			gameDayChannelsManager.start();
		} else {
			LOGGER.warn("Skipping Initialization of GameDayChannelsManager");
		}
	}

	/**
	 * This needs to be done in its own Thread. login().block() hold the execution.
	 * 
	 * @param botToken
	 */
	private void initDiscord(String botToken) {
		new Thread(() -> {
			LOGGER.info("Initializing Discord.");
			// Init DiscordClient and DiscordManager
			DiscordClient discordClient = DiscordClientBuilder.create(botToken)
					// globally suppress any not found (404) error
					.onClientResponse(ResponseFunction.emptyIfNotFound())
					// (403) Forbidden will not be retried.
					.onClientResponse(ResponseFunction.emptyOnErrorStatus(RouteMatcher.any(), 403))
					// (500) Error will be retried once.
					.onClientResponse(
							ResponseFunction.retryOnceOnErrorStatus(RouteMatcher.route(Routes.MESSAGE_CREATE), 500))
					.build();
			LOGGER.info("Logging into Discord.");
			GatewayDiscordClient gatewayDiscordClient = discordClient.login().block();

			LOGGER.info("Discord Client is ready.");
			discordManager.set(new DiscordManager(gatewayDiscordClient));
		}).start();
		LOGGER.info("Discord Initializer started.");
	}

	public void registerSlashCommands() {
		LOGGER.info("Update Slash Commands with Discord.");

		DiscordManager discordManager = getDiscordManager();

		List<Command> commands = getSlashCommands(this);

		long applicationId = discordManager.getApplicationId();
		RestClient restClient = discordManager.getClient().getRestClient();

		List<ApplicationCommandRequest> allCommands = commands.stream().filter(cmd -> cmd.getACR() != null)
				.map(Command::getACR).collect(Collectors.toList());

		List<ApplicationCommandRequest> commonCommands = commands.stream().filter(cmd -> cmd.getACR() != null)
				.filter(not(Command::isDevOnly)).map(Command::getACR).collect(Collectors.toList());

		// Dev Guilds
		for (Long guildId : Config.DEV_GUILD_LIST) {
			DiscordManager.block(restClient.getApplicationService().bulkOverwriteGuildApplicationCommand(applicationId,
					guildId, allCommands));
		}
		// Canucks Guild
		for (Long guildId : Config.SERVICED_GUILD_LIST) {
			DiscordManager.block(restClient.getApplicationService().bulkOverwriteGuildApplicationCommand(applicationId,
					guildId, commonCommands));
		}
	}

	private static void attachSlashCommandListeners(NHLBot nhlBot) {
		// Register Listeners
		for (Command command : getSlashCommands(nhlBot)) {
			LOGGER.debug("Registering Command listeners with client: " + command.getName());
			nhlBot.getDiscordManager().getClient().on(
					command)
					.doOnError(t -> LOGGER.error("Unable to respond to command: " + command.getName(), t))
					.onErrorResume(e -> Mono.empty()).subscribe();
		}
	}

	private static void attachListeners(NHLBot nhlBot) {
		LOGGER.info("Attaching Listeners.");

		Consumer<? super Throwable> logError = t -> LOGGER.error("Error occurred when responding to event.", t);

		nhlBot.getDiscordManager().getClient().getEventDispatcher().on(MessageCreateEvent.class).doOnError(logError)
				.onErrorResume(e -> Mono.empty()).subscribe(event -> nhlBot.getMessageListener().execute(event));
		nhlBot.getDiscordManager().getClient().getEventDispatcher().on(ReactionAddEvent.class).doOnError(logError)
				.onErrorResume(e -> Mono.empty()).subscribe(event -> nhlBot.getReactionListener().execute(event));
		nhlBot.getDiscordManager().getClient().getEventDispatcher().on(ReactionRemoveEvent.class).doOnError(logError)
				.onErrorResume(e -> Mono.empty()).subscribe(event -> nhlBot.getReactionListener().execute(event));
	}

	private void updateWelcomeChannel() {
		LOGGER.info("Updating 'Welcome' channels.");
		getDiscordManager().getClient().getGuilds()
				.filter(guild -> Config.DEV_GUILD_LIST.contains(guild.getId().asLong()))
				.subscribe(guild -> WelcomeChannel.createChannel(this, guild));
	}

	public PersistentData getPersistentData() {
		return persistantData;
	}

	public DiscordManager getDiscordManager() {
		return discordManager.get();
	}

	public GameScheduler getGameScheduler() {
		return gameScheduler;
	}

	public GameDayChannelsManager getGameDayChannelsManager() {
		return gameDayChannelsManager;
	}

	public MessageListener getMessageListener() {
		return messageListener;
	}

	public ReactionListener getReactionListener() {
		return reactionListener;
	}

	public GDCCategoryManager getGdcCategoryManager() {
		return gdcCategoryManager;
	}

	public WordcloudChannelManager getWordcloudChannelManager() {
		return wcChannelManager;
	}

	/**
	 * Gets the mention for the bot. It is how the raw message displays a mention of
	 * the bot's user.
	 * 
	 * @return
	 */
	public String getMention() {
		return "<@" + getDiscordManager().getId().asString() + ">";
	}

	/**
	 * Gets the id of the bot, in the format displayed in a message, when the bot is
	 * mentioned by Nickname.
	 * 
	 * @return
	 */
	public String getNicknameMention() {
		return "<@!" + getDiscordManager().getId().asString() + ">";
	}

	/*
	 * FOR DEPLOYMENT / TESTING PURPOSES ONLY.
	 * Create a NHLBot that is not started to access this.
	 */
	public void deployScript() {
		LOGGER.info("Deploy scripts executing...");
		registerSlashCommands();
		LOGGER.info("Deploy scripts completed.");
	}

	@SuppressWarnings("unused")
	private void deregisterGlobalSlashCommands() {
		long applicationId = getDiscordManager().getApplicationId();
		DiscordManager.block(getDiscordManager().getClient().getRestClient().getApplicationService()
				.bulkOverwriteGlobalApplicationCommand(applicationId, new ArrayList<>()));

	}

	@SuppressWarnings("unused")
	private List<ApplicationCommandData> getGlobalSlashCommandsInfo() {
		long applicationId = getDiscordManager().getApplicationId();
		return DiscordManager.block(getDiscordManager().getClient().getRestClient().getApplicationService()
				.getGlobalApplicationCommands(applicationId));
	}

	@SuppressWarnings("unused")
	private List<ApplicationCommandData> getGuildSlashCommandsInfo(long guildId) {
		long applicationId = getDiscordManager().getApplicationId();
		return DiscordManager.block(getDiscordManager().getClient().getRestClient()
				.getApplicationService().getGuildApplicationCommands(applicationId, guildId));
	}

	static List<Command> getSlashCommands(NHLBot nhlBot) {
		return Config.getSlashCommands().stream()
				.map(commandClass -> instantiateCommand(commandClass, nhlBot))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	@SuppressWarnings("rawtypes")
	static Command instantiateCommand(Class commandClass, NHLBot nhlBot) {
		if (!Command.class.isAssignableFrom(commandClass)) {
			LOGGER.warn("Non-Command class: " + commandClass.getSimpleName());
			return null;
		}
		try {
			return (Command) commandClass.getDeclaredConstructors()[0].newInstance(nhlBot);
		} catch (Exception e) {
			LOGGER.error("Could not load command: " + commandClass.getSimpleName());
			return null;
		}
	}
}
