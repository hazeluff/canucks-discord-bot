package com.hazeluff.discord.bot.command;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.stats.NHLDefenderStatsCommand;
import com.hazeluff.discord.bot.command.stats.NHLDivisionStatsCommand;
import com.hazeluff.discord.bot.command.stats.NHLForwardStatsCommand;
import com.hazeluff.discord.bot.command.stats.NHLGoalieStatsCommand;
import com.hazeluff.discord.bot.command.stats.NHLStatsSubCommand;
import com.hazeluff.discord.bot.command.stats.NHLWildcardStatsCommand;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Displays the score of a game in a Game Day Channel.
 */
public class NHLStatsCommand extends Command {
	static final String NAME = "stats";

	private static Map<String, NHLStatsSubCommand> PUBLIC_COMMANDS = 
			Arrays.asList(
					new NHLForwardStatsCommand(),
					new NHLDefenderStatsCommand(),
					new NHLGoalieStatsCommand(),
					new NHLDivisionStatsCommand(),
					new NHLWildcardStatsCommand()
			)
			.stream()
			.collect(Collectors.toMap(NHLStatsSubCommand::getName, UnaryOperator.identity()));

	public NHLStatsCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	public String getName() {
		return NAME;
	}

	public ApplicationCommandRequest getACR() {
		return ApplicationCommandRequest.builder()
				.name(getName())
				.description("Get Canucks league stats for the team and players.")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("type")
						.description("Type of stats to retrieve.")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.addAllChoices(CHOICES)
						.addChoice(buildChoice("help"))
						.required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("team")
						.description("3 Digit code of the Team.")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.required(false)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
						.name("season")
						.description("NHL Season Year eg. '2023'.")
						.type(ApplicationCommandOption.Type.INTEGER.getValue())
						.required(false)
                        .build())
				.build();
	}
	
	public static List<ApplicationCommandOptionChoiceData> CHOICES = PUBLIC_COMMANDS.entrySet().stream()
			.map(entry -> entry.getValue().getName())
			.map(Command::buildChoice)
			.collect(Collectors.toList());

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {
		String strSubcommand = InteractionUtils.getOptionAsString(event, "type");
		if (strSubcommand == null) {
			// No option specified
			InteractionApplicationCommandCallbackSpec spec = InteractionApplicationCommandCallbackSpec.builder()
					.addEmbed(HELP_MESSAGE_EMBED)
					.ephemeral(true)
					.build();
			return event.reply(spec);
		}

		NHLStatsSubCommand publicCommand = PUBLIC_COMMANDS.get(strSubcommand.toLowerCase());
		if (publicCommand != null) {
			return publicCommand.reply(event, nhlBot);
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
		builder.title("Stats Command");
		builder.description("Use `/" + NAME + " type:` to fetch league and player stats."
				+ "\nChoices default to Canucks and the current season.");

		// List the subcommands
		PUBLIC_COMMANDS.entrySet()
				.forEach(subCmd -> builder.addField(
						StringUtils.capitalize(subCmd.getKey()), 
						subCmd.getValue().getDescription(), 
						false
					)
				);
		return builder.build();
	}
}
