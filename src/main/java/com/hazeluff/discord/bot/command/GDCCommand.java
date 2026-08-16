package com.hazeluff.discord.bot.command;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.gdc.GDCGoalsCommand;
import com.hazeluff.discord.bot.command.gdc.GDCScoreCommand;
import com.hazeluff.discord.bot.command.gdc.GDCStatsCommand;
import com.hazeluff.discord.bot.command.gdc.GDCStatusCommand;
import com.hazeluff.discord.bot.command.gdc.GDCSubCommand;
import com.hazeluff.discord.bot.database.channel.gdc.GDCMeta;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.gdc.nhl.NHLGameDayWatchChannel;
import com.hazeluff.discord.nhl.NHLTeams.Team;
import com.hazeluff.nhl.game.NHLGame;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.ThreadChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Displays the score of a game in a Game Day Channel.
 */
public class GDCCommand extends Command {
	static final String NAME = "gdc";

	private static Map<String, GDCSubCommand> PUBLIC_COMMANDS = 
			Arrays.asList(
				new GDCScoreCommand(),
				new GDCGoalsCommand(),
				new GDCStatusCommand(),
				new GDCStatsCommand()
			)
			.stream()
			.collect(Collectors.toMap(GDCSubCommand::getName, UnaryOperator.identity()));

	public GDCCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	public String getName() {
		return NAME;
	}

	public ApplicationCommandRequest getACR() {
		return ApplicationCommandRequest.builder()
				.name(getName())
				.description("Get the score of the current game. Use only in Game Day Channels.")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("subcommand")
						.description("Subcommand to execute. Help: `/gdc subcommand:help`")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.addAllChoices(CHOICES)
						.required(false)
                        .build())
				.build();
	}

	private final static List<ApplicationCommandOptionChoiceData> CHOICES = PUBLIC_COMMANDS.entrySet().stream()
			.map(entry -> entry.getValue().getName())
			.map(name -> ApplicationCommandOptionChoiceData.builder()
					.name(StringUtils.capitalize(name))
					.value(name)
					.build())
			.collect(Collectors.toList());

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {

		/*
		 * Sub commands list
		 */
		String strSubcommand = getOptionAsString(event, "subcommand");
		if (strSubcommand == null) {
			// No option specified
			InteractionApplicationCommandCallbackSpec spec = InteractionApplicationCommandCallbackSpec.builder()
					.addEmbed(HELP_MESSAGE_EMBED)
					.ephemeral(true)
					.build();
			return event.reply(spec);
		}

		GDCSubCommand publicCommand = PUBLIC_COMMANDS.get(strSubcommand.toLowerCase());
		if(publicCommand == null) {
			InteractionApplicationCommandCallbackSpec spec = InteractionApplicationCommandCallbackSpec.builder()
				.addEmbed(HELP_MESSAGE_EMBED)
				.ephemeral(true)
				.build();
			return event.reply(spec);
		}

		long guildId = event.getInteraction().getGuildId().get().asLong();
		GuildPreferences preferences = nhlBot.getPersistentData().getPreferencesData()
			.getGuildPreferences(guildId);
		List<Team> teams = preferences.getTeams();
		if(teams.size() == 0) {
			return event.reply(MUST_BE_SUBSCRIBED_TO_TEAM_REPLY_SPEC);
		}
		else if (teams.size() > 1) {
			return event.reply(NOT_AVAILABLE_TO_MULTIPLE_TEAMS_REPLY_SPEC);
		}
		
		/*
		 * Get Game
		 */
		NHLGame game = null;

		// Individual Game Day Channel
		TextChannel channel = getTextChannel(event);
		if (channel != null)
			game = nhlBot.getNHLGameScheduler().getGameByChannelName(channel.getName());

		// Singular Game Day Channel
		if (game == null) {
			NHLGameDayWatchChannel gdwc = NHLGameDayWatchChannel.getChannel(guildId);
			if (gdwc != null) {
				// If Top Level Channel - match name
				if (channel != null && gdwc.getDiscordChannelName().equals(channel.getName())) {
					game = nhlBot.getNHLGameScheduler().getCurrentLiveGame(teams.get(0));
					if (game == null)
						game = nhlBot.getNHLGameScheduler().getNextGame(teams.get(0));
				}
				else if (channel == null) {
					ThreadChannel threadChannel = getThreadChannel(event);
					// If Thread
					if (threadChannel != null) {
						GDCMeta meta = nhlBot.getPersistentData().getGDCMetaData()
							.loadMetaByChannelId(threadChannel.getId().asLong());
						if(meta != null) {
							game = nhlBot.getNHLGameScheduler().getGameById(meta.getGameId());
						}
					}
				}
			}
		}
		// GDC Thread

		/*
		 * Public sub commands
		 */
		if (game != null) {
			System.out.println("game=" + game.getNiceName());
			return publicCommand.reply(event, nhlBot, game);
		}

		/*
		 * Not in GDC Not using
		 */
		// Not in game day channel
		InteractionApplicationCommandCallbackSpec spec = InteractionApplicationCommandCallbackSpec.builder()
			.content("GDC Commands must be used in a Game Day Channel.")
				.addEmbed(HELP_MESSAGE_EMBED)
				.ephemeral(true)
				.build();
		return event.reply(spec);
	}

	/*
	 * General
	 */
	public static final EmbedCreateSpec HELP_MESSAGE_EMBED = buildHelpMessageEmbed();

	private static EmbedCreateSpec buildHelpMessageEmbed() {
		EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
		builder.title("Game Day Channel - Commands");
		builder.description("Use `/gdc subcommand:` in to get live data about the current game."
						+ " Must be used in a Game Day Channel.");

		// List the subcommands
		PUBLIC_COMMANDS.entrySet()
				.forEach(subCmd -> builder.addField(
					subCmd.getKey(), 
					subCmd.getValue().getDescription(), 
					false)
				);
		return builder.build();
	}

	InteractionApplicationCommandCallbackSpec MUST_BE_SUBSCRIBED_TO_TEAM_REPLY_SPEC =
		InteractionApplicationCommandCallbackSpec.builder()
			.content(SUBSCRIBE_FIRST_MESSAGE)
			.ephemeral(true)
			.build();

	InteractionApplicationCommandCallbackSpec NOT_AVAILABLE_TO_MULTIPLE_TEAMS_REPLY_SPEC = 
		InteractionApplicationCommandCallbackSpec.builder()
			.content("This feature currently does not work when subscribed to multiple teams.")
			.ephemeral(true)
			.build();
}
