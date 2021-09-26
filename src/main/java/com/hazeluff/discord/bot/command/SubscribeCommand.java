package com.hazeluff.discord.bot.command;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.nhl.Team;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Permission;

/**
 * Subscribes guilds to a team.
 */
public class SubscribeCommand extends Command {

	public SubscribeCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	public String getName() {
		return "subscribe";
	}

	public ApplicationCommandRequest getACR() {
		return ApplicationCommandRequest.builder()
				.name(getName())
				.description("Subscribe the server to the guilds you want. Must be an Admin.")
				.build();
	}

	@Override
	public void execute(MessageCreateEvent event, CommandArguments command) {
		Guild guild = getGuild(event);
		Message message = event.getMessage();
		Member user = getMessageAuthor(message);
		if (!isOwner(guild, user)
				&& !hasPermissions(guild, user, Arrays.asList(Permission.MANAGE_CHANNELS, Permission.ADMINISTRATOR))) {
			sendMessage(event, MUST_HAVE_PERMISSIONS_MESSAGE);
			return;
		}

		if (command.getArguments().isEmpty()) {
			sendMessage(event, SPECIFY_TEAM_MESSAGE);
			return;
		}

		if (command.getArguments().get(0).equalsIgnoreCase("help")) {
			sendMessage(event, HELP_MESSAGE);
			return;
		}

		if (!Team.isValid(command.getArguments().get(0))) {
			sendMessage(event, getInvalidCodeMessage(command.getArguments().get(0), "subscribe"));
			return;
		}

		Team team = Team.parse(command.getArguments().get(0));
		// Subscribe guild
		long guildId = event.getGuildId().get().asLong();
		nhlBot.getGameDayChannelsManager().deleteInactiveGuildChannels(guild);
		nhlBot.getPersistentData().getPreferencesData().subscribeGuild(guildId, team);
		nhlBot.getGameDayChannelsManager().initChannels(guild);
		sendMessage(event, buildSubscribedMessage(team, guildId));
	}

	static final Consumer<MessageCreateSpec> MUST_HAVE_PERMISSIONS_MESSAGE = spec -> spec
			.setContent("You must have _Admin_ or _Manage Channels_ roles to subscribe the guild to a team.");
	static final Consumer<MessageCreateSpec> SPECIFY_TEAM_MESSAGE = spec -> spec
			.setContent("You must specify a parameter for what team you want to subscribe to. `?subscribe [team]`");
	static final Consumer<MessageCreateSpec> HELP_MESSAGE = spec -> {
		StringBuilder response = new StringBuilder(
				"Subscribed to any of the following teams by typing `?subscribe [team]`, "
						+ "where [team] is the one of the three letter codes for your team below: ").append("```");
		List<Team> teams = Team.getSortedLValues();
		for (Team team : teams) {
			response.append("\n").append(team.getCode()).append(" - ").append(team.getFullName());
		}
		response.append("```\n");
		response.append("You can unsubscribe using:\n");
		response.append("`?unsubscribe`");
		spec.setContent(response.toString());
	};

	Consumer<MessageCreateSpec> buildSubscribedMessage(Team team, long guildId) {
		List<Team> subscribedTeams = nhlBot.getPersistentData().getPreferencesData()
				.getGuildPreferences(guildId)
				.getTeams();
		if (subscribedTeams.size() > 1) {
			String teamsStr = StringUtils.join(subscribedTeams.stream().map(subbedTeam -> subbedTeam.getFullName())
					.sorted().collect(Collectors.toList()), "\n");
			return spec -> spec.setContent("This server is now subscribed to:\n```" + teamsStr + "```");
		} else {
			return spec -> spec
					.setContent("This server is now subscribed to games of the **" + team.getFullName() + "**!");
		}
	}


	@Override
	public boolean isAccept(Message message, CommandArguments command) {
		return command.getCommand().equalsIgnoreCase("subscribe");
	}

}
