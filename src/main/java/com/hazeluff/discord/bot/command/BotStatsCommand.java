package com.hazeluff.discord.bot.command;

import java.util.Arrays;
import java.util.List;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.bot.NHLBot;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Displays information about NHLBot and the author
 */
public class BotStatsCommand extends Command {
	private static final Logger LOGGER = LoggerFactory.getLogger(BotStatsCommand.class);

	static final String NAME = "svcstats";

	List<Long> excludedGuilds = Arrays.asList(
			264445053596991498l, // https://discordbots.org/
			110373943822540800l  // https://bots.discord.pw/
	);

	public BotStatsCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	public String getName() {
		return NAME;
	}

	public ApplicationCommandRequest getACR() {
		return null;
	}

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {
		return event.reply(buildReplyString());
	}

	public String buildReplyString() {
		String reply = "No guilds found...";
		List<Guild> guilds = nhlBot.getDiscordManager().getGuilds();
		if (guilds != null) {
			int numGuilds = guilds.size() - excludedGuilds.size();
			int numUsers = 0;
			for (Guild g : guilds) {
				try {
					if (!excludedGuilds.contains(g.getId().asLong())) {
						numUsers += g.getMemberCount();
					}
				} catch (Exception e) {
					LOGGER.warn("Exception happened.", e);
				}
			}
			reply = String.format("**Stats**\nGuilds: %s\nUsers: %s", numGuilds, numUsers);
		}
		return reply;
	}
}
