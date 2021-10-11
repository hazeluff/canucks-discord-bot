package com.hazeluff.discord.bot.command;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.gdc.GDCGoalsCommand;
import com.hazeluff.discord.bot.command.gdc.GDCScoreCommand;
import com.hazeluff.discord.bot.command.gdc.GDCSubCommand;
import com.hazeluff.nhl.Game;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Displays the score of a game in a Game Day Channel.
 */
public class GDCCommand extends Command {
	static final String NAME = "gdc";

	private static Map<String, GDCSubCommand> SUB_COMMANDS = Arrays
			.asList(
					new GDCScoreCommand(),
					new GDCGoalsCommand()
			).stream()
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
						.description("Subcommand to execute. Help: `/gdc subcommand: help`")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
				.build();
	}

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {
		TextChannel channel = getChannel(event);
		Game game = nhlBot.getGameScheduler().getGameByChannelName(channel.getName());
		if (game == null) {
			// Not in game day channel
			return event.reply(HELP_MESSAGE);
		}

		String strSubcommand = getOptionAsString(event, "subcommand");
		if (strSubcommand == null) {
			// No option specified
			return event.reply(HELP_MESSAGE);
		}
		GDCSubCommand subCommand = SUB_COMMANDS.get(strSubcommand.toLowerCase());
		if (subCommand != null) {
			return subCommand.reply(event, game);
		}

		return event.reply(HELP_MESSAGE);
	}

	/*
	 * General
	 */
	public static final Consumer<? super InteractionApplicationCommandCallbackSpec> HELP_MESSAGE = 
			callbackSpec -> callbackSpec
					.addEmbed(embedSpec -> embedSpec
							.setTitle("Game Day Channel - Commands")
							.setDescription("Use `/gdc subcommand:` in to get live data about the current game."
									+ " Must be used in a Game Day Channel.")
							.addField("score", "Get the game's current score.", false)
							.addField("goals", "Get the game's current score and goals.", false)
					)
					.setEphemeral(true);
	
}
