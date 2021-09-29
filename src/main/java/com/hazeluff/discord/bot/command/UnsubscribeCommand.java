package com.hazeluff.discord.bot.command;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.nhl.Team;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Permission;

/**
 * Unsubscribes guilds from a team.
 */
public class UnsubscribeCommand extends Command {

	static final String NAME = "unsubscribe";

	public UnsubscribeCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	public String getName() {
		return NAME;
	}

	public ApplicationCommandRequest getACR() {
		return ApplicationCommandRequest.builder()
				.name(getName())
				.description("Unsubscribe from Game Day Channels of the specified team. Must be an Admin.")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("team")
						.description("Team to subscribe to. 3 letter code - e.g. 'VAN'")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
				.build();
	}

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {
		Guild guild = getGuild(event);
		Member user = event.getInteraction().getMember().orElse(null);
		if (!isOwner(guild, user)
				&& !hasPermissions(guild, user, Arrays.asList(Permission.MANAGE_CHANNELS, Permission.ADMINISTRATOR))) {
			return event.replyEphemeral(MUST_HAVE_PERMISSIONS_MESSAGE);
		}

		String strTeam = getOptionAsString(event, "team");

		if (strTeam.equalsIgnoreCase("all")) {
			// Unsubscribe from all teams
			nhlBot.getPersistentData().getPreferencesData().unsubscribeGuild(guild.getId().asLong(), null);
			nhlBot.getGameDayChannelsManager().updateChannels(guild);
			return event.reply(UNSUBSCRIBED_FROM_ALL_MESSAGE);
		}

		if (!Team.isValid(strTeam)) {
			return event.replyEphemeral(getInvalidTeamCodeMessage(strTeam));
		}

		Team team = Team.parse(strTeam);
		// Subscribe guild
		nhlBot.getPersistentData().getPreferencesData().unsubscribeGuild(guild.getId().asLong(), team);
		nhlBot.getGameDayChannelsManager().updateChannels(guild);
		return event.reply(buildUnsubscribeMessage(team));
	}

	static final String HELP_MESSAGE = "Unsubscribe from Game Day Channels of the specified team."
			+ " Channels will be automatically removed."
			+ "[team] is the 3 letter code of the teams following:\n"
			+ HelpCommand.listOfTeams()
			+ "\nYou can unsubscribe from them to remove the channels using `/unsubscribe [team]`.";

	static final String MUST_HAVE_PERMISSIONS_MESSAGE = 
			"You must have _Admin_ or _Manage Channels_ roles to Unsubscribe the guild to a team.";
	
	static final Consumer<MessageCreateSpec> SPECIFY_TEAM_MESSAGE = spec -> spec
			.setContent("You must specify a parameter for what team you want to unsubscribe from. "
					+ "`?subscribe [team]`\n"
					+ "You may also use `?unsubscrube all` to unsubscribe from **all** teams.");

	String buildHelpMessage(Guild guild) {
		StringBuilder response = new StringBuilder(
				"Unsubscribe from any of your subscribed teams by typing `?unsubscribe [team]`, "
						+ "where [team] is the one of the three letter codes for your subscribed teams below: ")
								.append("```");
		List<Team> teams = nhlBot.getPersistentData()
				.getPreferencesData()
				.getGuildPreferences(guild.getId().asLong())
				.getTeams();
		for (Team team : teams) {
			response.append("\n").append(team.getCode()).append(" - ").append(team.getFullName());
		}
		response.append("all - all teams");
		response.append("```\n");
		return response.toString();
	}

	static String buildUnsubscribeMessage(Team team) {
		return "This server is now unsubscribed from games of the **" + team.getFullName() + "**.";
	}

	static final String UNSUBSCRIBED_FROM_ALL_MESSAGE = 
			"This server is now unsubscribed from games of all teams.";
}
