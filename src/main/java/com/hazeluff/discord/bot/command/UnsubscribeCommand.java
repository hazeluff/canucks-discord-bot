package com.hazeluff.discord.bot.command;

import java.util.List;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.gdc.nhl.NHLGameDayWatchChannel;
import com.hazeluff.discord.bot.gdc.nhl.NHLGdcGuildManager;
import com.hazeluff.discord.nhl.NHLTeams.Team;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

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
			return unsubscibeAllAndReply(event, guild);
		}

		if (!Team.isValid(strTeam)) {
			return reply(event, getInvalidTeamCodeMessage(strTeam), true);
		}

		Team team = Team.parse(strTeam);
		if (!team.isNHLTeam()) {
			return event.reply(NON_NHL_TEAM_MESSAGE).withEphemeral(true);
		}
		return unsubscibeAndReply(event, guild, team);
	}

	Mono<Message> unsubscibeAllAndReply(ChatInputInteractionEvent event, Guild guild) {
		return replyAndDeferEdit(event,
				"Unsubscribing...",
				() -> unsubscribeGuild(guild, null),
				() -> buildUnsubscribeAllReplyEdit(event, guild)
		);
	}

	Mono<Message> unsubscibeAndReply(ChatInputInteractionEvent event, Guild guild, Team team) {
		return replyAndDeferEdit(event,
				"Unsubscribing...",
				() -> unsubscribeGuild(guild, team),
				() -> buildUnsubscribeReplyEdit(event, guild, team)
		);
	}

	InteractionReplyEditSpec buildUnsubscribeAllReplyEdit(ChatInputInteractionEvent event, Guild guild) {
		return InteractionUtils.buildReplyEditSpec(UNSUBSCRIBED_FROM_ALL_MESSAGE);
	}

	InteractionReplyEditSpec buildUnsubscribeReplyEdit(ChatInputInteractionEvent event, Guild guild, Team team) {
		return InteractionUtils.buildReplyEditSpec(buildUnsubscribeMessage(team));
	}

	static final String HELP_MESSAGE = "Unsubscribe from Game Day Channels of the specified team."
			+ " Channels will be automatically removed."
			+ "[team] is the 3 letter code of the teams following:\n"
			+ HelpCommand.listOfTeams()
			+ "\nYou can unsubscribe from them to remove the channels using `/unsubscribe [team]`.";
	
	static final String SPECIFY_TEAM_MESSAGE = 
			"You must specify a parameter for what team you want to unsubscribe from. "
					+ "`?subscribe [team]`\n"
					+ "You may also use `?unsubscrube all` to unsubscribe from **all** teams.";

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

	private void unsubscribeGuild(Guild guild, Team team) {
		long guildId = guild.getId().asLong();

		// Remove the team from the Guild Preferences
		GuildPreferences pref = nhlBot.getPersistentData().getPreferencesData().unsubscribeGuild(guildId, team);

		if (pref.isSingleNHLChannel() || Config.isDevGuild(guild)) {
			NHLGameDayWatchChannel channel = NHLGameDayWatchChannel.getChannel(guildId);
			List<Team> teams = pref.getTeams();
			if (channel == null && !teams.isEmpty()) {
				channel = NHLGameDayWatchChannel.getOrCreate(nhlBot, guild);
			} else if (teams.isEmpty()) {
				NHLGameDayWatchChannel.removeChannel(guildId);
			} else if (channel != null) {
				channel.update(pref);
			}

		} else if (pref.isChannelPerNHLGame() || Config.isDevGuild(guild)) {
			NHLGdcGuildManager manager = NHLGdcGuildManager.getManager(guildId);
			List<Team> teams = pref.getTeams();
			if (manager == null && !teams.isEmpty()) {
				manager = NHLGdcGuildManager.getAndStart(nhlBot, guild);
			} else if (teams.isEmpty()) {
				NHLGdcGuildManager.removeManager(guildId);
			} else if (manager != null) {
				manager.updateChannels(pref);
			}
		}
	}

	static String buildUnsubscribeMessage(Team team) {
		return "This server is now unsubscribed from games of the **" + team.getFullName() + "**.";
	}

	static final String UNSUBSCRIBED_FROM_ALL_MESSAGE = 
			"This server is now unsubscribed from games of all teams.";
}
