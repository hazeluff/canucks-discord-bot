package com.hazeluff.discord.bot.command;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.gdc.GDCGoalsCommand;
import com.hazeluff.discord.bot.command.gdc.GDCScoreCommand;
import com.hazeluff.discord.bot.command.gdc.GDCStatsCommand;
import com.hazeluff.discord.bot.command.gdc.GDCStatusCommand;
import com.hazeluff.discord.bot.command.gdc.GDCSubCommand;
import com.hazeluff.discord.bot.gdc.nhl.NHLGameDayWatchChannel;
import com.hazeluff.discord.bot.gdc.nhl.fournations.FourNationsWatchChannel;
import com.hazeluff.discord.nhl.NHLTeams.Team;
import com.hazeluff.nhl.game.Game;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.TextChannel;
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
		TextChannel channel = getChannel(event);
		if (channel.getName().equals(FourNationsWatchChannel.CHANNEL_NAME)) {
			// Not in game day channel
			InteractionApplicationCommandCallbackSpec spec = InteractionApplicationCommandCallbackSpec.builder()
					.content("GDC Commands not supported for Four Nations channel.")
					.ephemeral(true)
					.build();
			return event.reply(spec);
		}

		if (channel.getName() != NHLGameDayWatchChannel.CHANNEL_NAME) {
			// Not in game day channel
			InteractionApplicationCommandCallbackSpec spec = InteractionApplicationCommandCallbackSpec.builder()
					.content("GDC Commands must be used in a Game Day Channel.")
					.addEmbed(HELP_MESSAGE_EMBED)
					.ephemeral(true)
					.build();
			return event.reply(spec);
		}

		List<Team> teams = nhlBot.getPersistentData().getPreferencesData()
				.getGuildPreferences(event.getInteraction().getGuildId().get().asLong()).getTeams();
		if(teams.size() > 1) {
			InteractionApplicationCommandCallbackSpec spec = InteractionApplicationCommandCallbackSpec.builder()
					.content("Your server can only be subscribed to a single team to use this feature.")
					.ephemeral(true)
					.build();
			return event.reply(spec);
		} else if (teams.size() == 0) {
			InteractionApplicationCommandCallbackSpec spec = InteractionApplicationCommandCallbackSpec.builder()
					.content("Your server must be subscribed to a single team to use this feature.")
					.ephemeral(true)
					.build();
			return event.reply(spec);
		} // else teams.size() == 1
		
		
		/*
		 * Sub commands
		 */
		String strSubcommand = InteractionUtils.getOptionAsString(event, "subcommand");
		if (strSubcommand == null) {
			// No option specified
			InteractionApplicationCommandCallbackSpec spec = InteractionApplicationCommandCallbackSpec.builder()
					.addEmbed(HELP_MESSAGE_EMBED)
					.ephemeral(true)
					.build();
			return event.reply(spec);
		}

		/*
		 * Public sub commands
		 */
		GDCSubCommand publicCommand = PUBLIC_COMMANDS.get(strSubcommand.toLowerCase());
		if (publicCommand != null) {
			Game game = nhlBot.getNHLGameScheduler().getCurrentLiveGame(teams.get(0));
			return publicCommand.reply(event, nhlBot, game);
		}

		InteractionApplicationCommandCallbackSpec spec = InteractionApplicationCommandCallbackSpec.builder()
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

}
