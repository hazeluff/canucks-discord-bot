package com.hazeluff.discord.bot.command;

import java.util.List;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.nhl.Team;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

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
		if (!hasPrivilege(guild, user)) {
			return reply(event, MUST_HAVE_PERMISSIONS_MESSAGE);
		}

		String strTeam = getOptionAsString(event, "team");

		if (strTeam.equalsIgnoreCase("all")) {
			// Unsubscribe from all teams
			return replyAndDefer(event, "Unsubscribing...", () -> buildUnsubscribeAllFollowUp(event, guild));
		}

		if (!Team.isValid(strTeam)) {
			return reply(event, getInvalidTeamCodeMessage(strTeam), true);
		}

		Team team = Team.parse(strTeam);
		return replyAndDefer(event, "Unsubscribing...", () -> buildUnsubscribeFollowUp(event, guild, team));
	}

	InteractionFollowupCreateSpec buildUnsubscribeAllFollowUp(ChatInputInteractionEvent event, Guild guild) {
		unsubscribeGuild(guild, null);
		return InteractionFollowupCreateSpec.builder().content(UNSUBSCRIBED_FROM_ALL_MESSAGE).build();
	}

	InteractionFollowupCreateSpec buildUnsubscribeFollowUp(ChatInputInteractionEvent event, Guild guild, Team team) {
		unsubscribeGuild(guild, team);
		return InteractionFollowupCreateSpec.builder().content(buildUnsubscribeMessage(team)).build();
	}

	static final String HELP_MESSAGE = "Unsubscribe from reminders of the specified team."
			+ " Channels will be automatically removed."
			+ "[team] is the 3 letter code of the teams following:\n"
			+ HelpCommand.listOfTeams();
	
	static final String SPECIFY_TEAM_MESSAGE = 
			"You must specify a parameter for what team you want to unsubscribe from. "
					+ "`/subscribe team:[team]`\n"
					+ "You may also use `/unsubscrube team:all` to unsubscribe from **all** teams.";

	String buildHelpMessage(Guild guild) {
		StringBuilder response = new StringBuilder(
				"Unsubscribe from any of your subscribed teams by typing `/unsubscribe team:[team]`, "
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

	private void unsubscribeGuild(Guild guild, Team team) {
		nhlBot.getPersistentData().getPreferencesData().unsubscribeGuild(guild.getId().asLong(), team);
		nhlBot.getGameDayChannelsManager().updateGuild(guild);
	}

	static String buildUnsubscribeMessage(Team team) {
		return "This server is now unsubscribed from games of the **" + team.getFullName() + "**.";
	}

	static final String UNSUBSCRIBED_FROM_ALL_MESSAGE = 
			"This server is now unsubscribed from games of all teams.";
}
