package com.hazeluff.discord.bot.command;


import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.reactivestreams.Publisher;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.GameDayChannel;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.nhl.Game;
import com.hazeluff.discord.nhl.Team;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Interface for commands that the NHLBot can accept and the replies to those
 * commands.
 * </p>
 * 
 * <p>
 * New Commands should be added to {@link NHLBot#getCommands}
 * </p>
 * 
 */
public abstract class Command extends ReactiveEventAdapter {
	static final String SUBSCRIBE_FIRST_MESSAGE = 
			"Please have your admin first subscribe your guild "
					+ "to a team by using the command `@NHLBot subscribe [team]`, "
					+ "where [team] is the 3 letter code for your team.\n"
					+ "To see a list of [team] codes use command `?subscribe help`";
	static final String GAME_NOT_STARTED_MESSAGE = 
			"The game hasn't started yet.";
	static final String RUN_IN_SERVER_CHANNEL_MESSAGE = 
			"This can only be run on a server's 'Game Day Channel'.";
	
	protected final NHLBot nhlBot;

	Command(NHLBot nhlBot) {
		this.nhlBot = nhlBot;
	}

	public abstract String getName();
	public abstract ApplicationCommandRequest getACR();

	public boolean isDevOnly() {
		return false;
	}

	public abstract Publisher<?> onChatCommandInput(ChatInputInteractionEvent event);

	public Publisher<?> onChatInputInteraction(ChatInputInteractionEvent event) {
		// Filters out commands that are not intended for the implementing Command class.
		if (event.getCommandName().equals(getName())) {
			return onChatCommandInput(event);
		}
		return Mono.empty();
	}

	protected void sendMessage(MessageCreateEvent event, String message) {
		sendMessage(event, spec -> spec.setContent(message));
	}

	protected void sendMessage(MessageCreateEvent event, Consumer<MessageCreateSpec> spec) {
		TextChannel channel = (TextChannel) nhlBot.getDiscordManager().block(event.getMessage().getChannel());
		sendMessage(channel, spec);
	}

	protected void sendMessage(TextChannel channel, Consumer<MessageCreateSpec> spec) {
		nhlBot.getDiscordManager().sendMessage(channel, spec);
	}

	/**
	 * Gets the channel (mention) in the specified guild that represents the latest game of the team that guild is
	 * subscribed to.
	 * 
	 * @param guild
	 *            guild where the channels are in
	 * @param team
	 *            team to get latest game of
	 * @return channel of the latest game
	 */
	String getLatestGameChannelMention(Guild guild, Team team) {
		Game game = getLatestGame(team);
		if (game == null) {
			return null;
		}
		String channelName = GameDayChannel.getChannelName(game).toLowerCase();

		List<TextChannel> channels = nhlBot.getDiscordManager().getTextChannels(guild);
		if(channels != null && !channels.isEmpty()) {
			TextChannel channel = channels.stream()
					.filter(chnl -> chnl.getName().equalsIgnoreCase(channelName))
					.findAny()
					.orElse(null);
			if (channel != null) {
				return channel.getMention();
			}
		}

		return "#" + channelName;
	}

	Game getLatestGame(Team team) {
		Game game = nhlBot.getGameScheduler().getCurrentGame(team);
		if (game == null) {
			game = nhlBot.getGameScheduler().getLastGame(team);
		}
		return game;
	}

	/**
	 * Gets message to send when a command needs to be run in a 'Game Day Channel'.
	 * 
	 * @param channel
	 * @param team
	 * @return
	 */
	String getRunInGameDayChannelsMessage(Guild guild, List<Team> teams) {
		String channelMentions = getLatestGamesListString(guild, teams);
		return String.format("Please run this command in a 'Game Day Channel'.\nLatest game channel(s): %s",
				channelMentions);
	}

	String getLatestGamesListString(Guild guild, List<Team> teams) {
		return StringUtils.join(
				teams.stream().map(team -> getLatestGameChannelMention(guild, team)).collect(Collectors.toList()),
				", ");
	}

	boolean hasPermissions(Guild guild, Member user, List<Permission> permissions) {
		if (user == null) {
			return false;
		}
		PermissionSet permissionsSet = getPermissions(user);
		if (permissionsSet == null) {
			return false;
		}
		return permissions.stream().allMatch(permissionsSet::contains);
	}

	Member getMessageAuthor(Message message) {
		return nhlBot.getDiscordManager().block(message.getAuthorAsMember());
	}

	PermissionSet getPermissions(Member user) {
		return nhlBot.getDiscordManager().block(user.getBasePermissions());
	}

	boolean isOwner(Guild guild, User user) {
		if (user == null) {
			return false;
		}
		return guild.getOwner().block().getId().equals(user.getId());
	}

	boolean isDev(Snowflake userId) {
		return userId.asLong() == Config.HAZELUFF_ID;
	}

	/**
	 * Gets the message that specifies the inputted Team code was incorrect. Command
	 * using this should implement the help function.
	 * 
	 * @param channel
	 *            channel to send the message to
	 * @param incorrectCode
	 *            the incorrect code the user inputed
	 * @param command
	 *            command to tell user to invoke help of
	 * @return
	 */
	String getInvalidTeamCodeMessage(String incorrectCode) {
		return String.format("`%s` is not a valid team code.\nUse `/help teams` to get a full list of team",
				incorrectCode);
	}

	/**
	 * Gets the subscribed Team of the User/Guild in the message.
	 * 
	 * @param message
	 *            the source message
	 * @return the subscribed team for the User/Guild
	 */
	List<Team> getTeams(Guild guild) {
		return nhlBot.getPersistentData()
				.getPreferencesData()
				.getGuildPreferences(guild.getId().asLong())
				.getTeams();
	}

	/**
	 * Gets a string that creates a quote/code block in Discord listing all the NHL
	 * teams and their codes.
	 * 
	 * @return
	 */
	static String getTeamsListBlock() {
		StringBuilder strBuilder = new StringBuilder("```");
		for (Team team : Team.values()) {
			strBuilder.append("\n").append(team.getCode()).append(" - ").append(team.getFullName());
		}
		strBuilder.append("```\n");
		return strBuilder.toString();
	}

	protected Guild getGuild(ChatInputInteractionEvent event) {
		return nhlBot.getDiscordManager().block(event.getInteraction().getGuild());
	}

	protected TextChannel getChannel(ChatInputInteractionEvent event) {
		return nhlBot.getDiscordManager().block(event.getInteraction().getChannel().cast(TextChannel.class));
	}

	protected static String getOptionAsString(ChatInputInteractionEvent event, String option) {
		return event.getInteraction().getCommandInteraction().get().getOption(option)
				.flatMap(ApplicationCommandInteractionOption::getValue)
				.map(ApplicationCommandInteractionOptionValue::asString).orElse(null);
	}

	protected static Long getOptionAsLong(ChatInputInteractionEvent event, String option) {
		return event.getInteraction().getCommandInteraction().get().getOption(option)
				.flatMap(ApplicationCommandInteractionOption::getValue)
				.map(ApplicationCommandInteractionOptionValue::asLong).orElse(null);
	}
}
