package com.hazeluff.discord.bot.command;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.nhl.Team;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Subscribes guilds to a team.
 */
public class SubscribeCommand extends Command {

	static final String NAME = "subscribe";

	public SubscribeCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	public String getName() {
		return NAME;
	}

	public ApplicationCommandRequest getACR() {
		return ApplicationCommandRequest.builder()
				.name(getName())
				.description("Subscribe to Game Day Channels of the specified team. Must be an Admin.")
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
			return event.reply(MUST_HAVE_PERMISSIONS_MESSAGE);
		}

		String strTeam = getOptionAsString(event, "team");

		if (!Team.isValid(strTeam)) {
			return event.replyEphemeral(getInvalidTeamCodeMessage(strTeam));
		}

		Team team = Team.parse(strTeam);
		// Subscribe guild
		long guildId = guild.getId().asLong();
		nhlBot.getPersistentData().getPreferencesData().subscribeGuild(guildId, team);
		nhlBot.getGameDayChannelsManager().updateChannels(guild);
		return event.reply(buildSubscribedMessage(team, guildId));
	}

	static final String HELP_MESSAGE = "Subscribe to create Game Day Channels of the most recent games for that team."
			+ " Channels will be automatically added and removed each night."
			+ "[team] is the 3 letter code of the teams following:\n"
			+ HelpCommand.listOfTeams()
			+ "\nYou can unsubscribe from them to remove the channels using `/unsubscribe [team]`.";

	String buildSubscribedMessage(Team team, long guildId) {
		List<Team> subscribedTeams = nhlBot.getPersistentData().getPreferencesData()
				.getGuildPreferences(guildId)
				.getTeams();
		if (subscribedTeams.size() > 1) {
			String teamsStr = StringUtils.join(subscribedTeams.stream().map(subbedTeam -> subbedTeam.getFullName())
					.sorted().collect(Collectors.toList()), "\n");
			return "This server is now subscribed to:\n```" + teamsStr + "```";
		} else {
			return "This server is now subscribed to games of the **" + team.getFullName() + "**!";
		}
	}
}
