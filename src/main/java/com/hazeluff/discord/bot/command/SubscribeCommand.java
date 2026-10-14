package com.hazeluff.discord.bot.command;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.gdc.nhl.NHLGameDayWatchChannel;
import com.hazeluff.discord.bot.gdc.nhl.NHLGdcGuildManager;
import com.hazeluff.discord.nhl.NHLTeams.Team;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Subscribes guilds to a team.
 */
public class SubscribeCommand extends Command {
	private static final Logger LOGGER = LoggerFactory.getLogger(SubscribeCommand.class);

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
				.description("Subscribe to games of the specified NHL team."
					+ " Use this on your own server.")
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
		String strTeam = getOptionAsString(event, "team");
		LOGGER.info("Subscribing guild to team. guildId={}, team={}", guild.getId().asLong(), strTeam);

		Member user = event.getInteraction().getMember().orElse(null);
		if (!hasPrivilege(guild, user)) {
			return event.reply(MUST_HAVE_PERMISSIONS_MESSAGE);

		}

		if (!Team.isValid(strTeam)) {
			return event.reply(getInvalidTeamCodeMessage(strTeam)).withEphemeral(true);
		}

		Team team = Team.parse(strTeam);
		if (!team.isNHLTeam()) {
			return event.reply(NON_NHL_TEAM_MESSAGE).withEphemeral(true);
		}

		return Command.replyAndDeferEdit(event,
				"Subscribing...",
				() -> subscribeGuild(guild, team),
				() -> buildReplyEdit(guild, team)
		);
	}

	InteractionReplyEditSpec buildReplyEdit(Guild guild, Team team) {
		return InteractionReplyEditSpec.builder().contentOrNull(buildSubscribedMessage(guild, team)).build();
	}

	static final String HELP_MESSAGE = "Subscribe to create Game Day Channels of the most recent games for that team."
			+ " Channels will be automatically added and removed each night."
			+ "[team] is the 3 letter code of the teams following:\n"
			+ HelpCommand.listOfTeams()
			+ "\nYou can unsubscribe from them to remove the channels using `/unsubscribe [team]`.";

	private void subscribeGuild(Guild guild, Team team) {
		Long guildId = guild.getId().asLong();
		// Update preferences
		GuildPreferences pref = nhlBot.getPersistentData().getPreferencesData().subscribeGuild(guildId, team);

		if (pref.getGDCMode().isSingleChannel()) {
			NHLGameDayWatchChannel channel = NHLGameDayWatchChannel.getChannel(guildId);
			if (channel == null) {
				channel = NHLGameDayWatchChannel.getOrCreate(nhlBot, guild);
			} else {
				channel.updateGameThreads(pref);
			}
		}
		else if (pref.getGDCMode().isIndvidualChannels()) {
			NHLGdcGuildManager gdcManager = NHLGdcGuildManager.getManager(guildId);
			if (gdcManager == null) {
				gdcManager = NHLGdcGuildManager.getAndStart(nhlBot, guild);
			} else {
				gdcManager.updateChannels(pref);
			}
		}
	}

	String buildSubscribedMessage(Guild guild, Team team) {
		List<Team> subscribedTeams = nhlBot.getPersistentData().getPreferencesData()
				.getGuildPreferences(guild.getId().asLong())
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
