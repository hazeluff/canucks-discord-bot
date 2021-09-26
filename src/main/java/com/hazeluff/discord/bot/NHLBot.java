package com.hazeluff.discord.bot;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.channel.GDCCategoryManager;
import com.hazeluff.discord.bot.channel.WordcloudChannelManager;
import com.hazeluff.discord.bot.command.AboutCommand;
import com.hazeluff.discord.bot.command.Command;
import com.hazeluff.discord.bot.command.FuckCommand;
import com.hazeluff.discord.bot.command.GoalsCommand;
import com.hazeluff.discord.bot.command.HelpCommand;
import com.hazeluff.discord.bot.command.NextGameCommand;
import com.hazeluff.discord.bot.command.PredictionsCommand;
import com.hazeluff.discord.bot.command.ScheduleCommand;
import com.hazeluff.discord.bot.command.ScoreCommand;
import com.hazeluff.discord.bot.command.StatsCommand;
import com.hazeluff.discord.bot.command.SubscribeCommand;
import com.hazeluff.discord.bot.command.ThreadsCommand;
import com.hazeluff.discord.bot.command.UnsubscribeCommand;
import com.hazeluff.discord.bot.command.WordcloudCommand;
import com.hazeluff.discord.bot.database.PersistentData;
import com.hazeluff.discord.bot.discord.DiscordManager;
import com.hazeluff.discord.bot.listener.MessageListener;
import com.hazeluff.discord.bot.listener.ReactionListener;
import com.hazeluff.discord.nhl.GameScheduler;
import com.hazeluff.discord.utils.Utils;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.gateway.StatusUpdate;
import discord4j.rest.RestClient;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.request.RouteMatcher;
import discord4j.rest.response.ResponseFunction;
import discord4j.rest.route.Routes;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.retry.Retry;

public class NHLBot extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(NHLBot.class);

	private static final StatusUpdate STARTING_UP_STATUS = Presence.doNotDisturb(Activity.watching("itself start up"));
	private static final StatusUpdate ONLINE_PRESENCE = Presence.online(Activity.playing(Config.STATUS_MESSAGE));

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

	NHLBot(DiscordManager discordManager, PersistentData preferencesManager,
			GameScheduler gameScheduler, GameDayChannelsManager gameDayChannelsManager) {
		this.discordManager.set(discordManager);
		this.persistantData = preferencesManager;
		this.gameScheduler = gameScheduler;
		this.gameDayChannelsManager = gameDayChannelsManager;
	}

	public static NHLBot create(GameScheduler gameScheduler, String botToken) {
		LOGGER.info("Running NHLBot v" + Config.VERSION);
		Thread.currentThread().setName("NHLBot");

		NHLBot nhlBot = new NHLBot();
		nhlBot.gameScheduler = gameScheduler;

		// Init MongoClient/GuildPreferences
		nhlBot.initPersistentData();
		// Init Discord Client
		nhlBot.initDiscord(botToken);

		while (nhlBot.getDiscordManager() == null) {
			LOGGER.info("Waiting for Discord client to be ready.");
			Utils.sleep(5000);
		}

		// Set starting up status
		nhlBot.getDiscordManager().changePresence(STARTING_UP_STATUS);

		// Attach Listeners
		attachListeners(nhlBot);

		LOGGER.info("NHLBot Started. id [" + nhlBot.getDiscordManager().getId() + "]");

		List<Guild> guilds = nhlBot.getDiscordManager().getGuilds();

		// Start the Game Day Channels Manager
		nhlBot.initGameDayChannelsManager();

		// Init Static Entities (They must be init in order!!!)
		nhlBot.gdcCategoryManager.init(guilds);
		nhlBot.wcChannelManager.init(guilds);

		// Add 
		LOGGER.info("Add Commands.");
		addCommands(nhlBot);
		
		// Manage WelcomeChannels
		LOGGER.info("Posting update to Discord channel.");
		getPrivilegedGuilds(nhlBot.getDiscordManager())
			.map(Guild::getChannels)
			.filter(TextChannel.class::isInstance)
			.flatMap(channels -> channels.filter(channel -> 
					channel.getName().equals("welcome")).take(1))
			.cast(TextChannel.class)
			.subscribe(
					channel -> WelcomeChannel.create(nhlBot, channel),
					t -> LOGGER.error("Error occurred when starting WelcomeChannel.", t));

		while (!nhlBot.getGameScheduler().isInit()) {
			LOGGER.info("Waiting for GameScheduler...");
			Utils.sleep(2000);
		}

		nhlBot.getDiscordManager().changePresence(ONLINE_PRESENCE);

		nhlBot.start();

		return nhlBot;
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
	                // (500) while creating a message will be retried, with backoff, until it succeeds
	                .onClientResponse(ResponseFunction.retryWhen(RouteMatcher.route(Routes.MESSAGE_CREATE),
	                        Retry.onlyIf(ClientException.isRetryContextStatusCode(500))
									.retryMax(3)
	                        		.fixedBackoff(Duration.ofSeconds(5))))
					.build();

			LOGGER.info("Logging into Discord.");
			GatewayDiscordClient gatewayDiscordClient = discordClient.login().block();
			
			LOGGER.info("Discord Client is ready.");
			discordManager.set(new DiscordManager(gatewayDiscordClient));
		}).start();
		LOGGER.info("Discord Initializer started.");
	}
	
	private static void addCommands(NHLBot nhlBot) {
		DiscordManager discordManager = nhlBot.getDiscordManager();
		
		// List of all enabled commands. I'm not doing reflections so add it here when you make a new one, dummy.
		List<Command> commands = Arrays.asList(
			new AboutCommand(nhlBot),
			new FuckCommand(nhlBot),
			new GoalsCommand(nhlBot),
			new HelpCommand(nhlBot),
			new NextGameCommand(nhlBot),
			new PredictionsCommand(nhlBot),
			new ScoreCommand(nhlBot),
			new SubscribeCommand(nhlBot),
			new ScheduleCommand(nhlBot),
			new StatsCommand(nhlBot),
			new ThreadsCommand(nhlBot),
			new UnsubscribeCommand(nhlBot),
			new WordcloudCommand(nhlBot)					
		);
		
		long applicationId = discordManager.getApplicationId();
		RestClient restClient = discordManager.getClient().getRestClient();
		List<Snowflake> guilds = discordManager.block(getPrivilegedGuilds(discordManager).map(Guild::getId));

		for (Snowflake guild : guilds) {
			for (Command command : commands) {
				ApplicationCommandRequest acr = command.getACR();
				// Only register commands with ACR
				if(acr != null) {
					// Register Command
					restClient.getApplicationService()
							.createGuildApplicationCommand(applicationId, guild.asLong(), acr)
							.doOnError(t -> LOGGER.error("Unable to create guild command.", t))
							.onErrorResume(e -> Mono.empty())
							.block();

					// Register Listener for the command
					discordManager.getClient()
							.on(command)
							.doOnError(t -> LOGGER.error("Unable to respond to command.", t))
							.onErrorResume(e -> Mono.empty())
							.blockLast();
				}

			}
		}
	}
	
	private static void attachListeners(NHLBot nhlBot) {
		LOGGER.info("Attaching Listeners.");
		
		Consumer<? super Throwable> logError = t -> LOGGER.error("Error occurred when responding to message.", t);
		
		nhlBot.getDiscordManager().getClient().getEventDispatcher()
				.on(MessageCreateEvent.class)
				.doOnError(logError)
		        .onErrorResume(e -> Mono.empty())
				.subscribe(event -> nhlBot.getMessageListener().execute(event));
		nhlBot.getDiscordManager().getClient().getEventDispatcher()
				.on(ReactionAddEvent.class)
				.doOnError(logError)
		        .onErrorResume(e -> Mono.empty())
				.subscribe(event -> nhlBot.getReactionListener().execute(event));
		nhlBot.getDiscordManager().getClient().getEventDispatcher()
				.on(ReactionRemoveEvent.class)
				.doOnError(logError)
		        .onErrorResume(e -> Mono.empty())
				.subscribe(event -> nhlBot.getReactionListener().execute(event));
	}

	@Override
	public void run() {
		while (!isInterrupted()) {
			getDiscordManager().changePresence(ONLINE_PRESENCE);
			Utils.sleep(UPDATE_PLAY_STATUS_INTERVAL);
		}
	}

	void initPersistentData() {
		LOGGER.info("Initializing Persistent Data.");
		this.persistantData = PersistentData.load();
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

	// Helpers
	private static Flux<Guild> getPrivilegedGuilds(DiscordManager discordManager) {
		return discordManager.getClient().getGuilds()
				.filter(guild -> Config.GUILD_PRIVILEGED_LIST.contains(guild.getId().asLong()));
	}
}
