package com.hazeluff.discord.bot.command;

import java.util.Arrays;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.command.gdc.GDCGoalsCommand;
import com.hazeluff.discord.bot.command.gdc.GDCScoreCommand;
import com.hazeluff.discord.bot.command.gdc.GDCStatusCommand;
import com.hazeluff.discord.bot.command.gdc.GDCSubCommand;
import com.hazeluff.discord.bot.gdc.GameDayChannel;
import com.hazeluff.nhl.game.Game;
import com.hazeluff.nhl.game.data.LiveDataException;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

/**
 * Displays the score of a game in a Game Day Channel.
 */
public class GDCCommand extends Command {
	static final String NAME = "gdc";

	private static Map<String, GDCSubCommand> SUB_COMMANDS = Arrays
			.asList(
					new GDCScoreCommand(),
					new GDCGoalsCommand(),
					new GDCStatusCommand()
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
						.description("Subcommand to execute. Help: `/gdc subcommand:help`")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.required(false)
                        .build())
				.build();
	}

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {
		TextChannel channel = getChannel(event);
		Game game = nhlBot.getGameScheduler().getGameByChannelName(channel.getName());
		if (game == null) {
			// Not in game day channel
			InteractionApplicationCommandCallbackSpec spec = InteractionApplicationCommandCallbackSpec.builder()
					.addEmbed(HELP_MESSAGE_EMBED)
					.ephemeral(true)
					.build();
			return event.reply(spec);
		}

		/*
		 * Sub commands
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

		/*
		 * Dev only sub commands
		 */
		if (strSubcommand.equals("sync")) {
			Member user = event.getInteraction().getMember().orElse(null);
			if (user != null && !isDev(user.getId())) {
				return event.reply(MUST_HAVE_PERMISSIONS_MESSAGE).withEphemeral(true);
			}
			return event.deferReply().then(syncGameAndFollowup(event, game));
		}

		if (strSubcommand.equals("refresh")) {
			Member user = event.getInteraction().getMember().orElse(null);
			if (user != null && !isDev(user.getId())) {
				return deferReply(event, MUST_HAVE_PERMISSIONS_MESSAGE);
			}
			return event.deferReply().then(refreshChannelAndFollowup(event, game));
		}

		/*
		 * Public sub commands
		 */
		GDCSubCommand subCommand = SUB_COMMANDS.get(strSubcommand.toLowerCase());
		if (subCommand != null) {
			return subCommand.reply(event, nhlBot, game);
		}
		InteractionApplicationCommandCallbackSpec spec = InteractionApplicationCommandCallbackSpec.builder()
				.addEmbed(HELP_MESSAGE_EMBED)
				.ephemeral(true)
				.build();
		return event.reply(spec);
	}

	private Mono<Message> syncGameAndFollowup(ChatInputInteractionEvent event, Game game) {
		try {
			game.resetLiveData();
		} catch (LiveDataException e) {
			return event.createFollowup("Failed to sync game data.");
		}
		return event.createFollowup("Synced game data.");
	}

	private Mono<Message> refreshChannelAndFollowup(ChatInputInteractionEvent event, Game game) {
		long guildId = event.getInteraction().getGuildId().get().asLong();
		GameDayChannel gameDayChannel = nhlBot.getGameDayChannelsManager().getGameDayChannel(guildId, game.getGamePk());
		if (gameDayChannel != null) {
			try {
				game.resetLiveData();
			} catch (LiveDataException e) {
				return event.createFollowup("Failed to sync game data.");
			}
			gameDayChannel.refreshMessages();
			return event.createFollowup("Channel refreshed.");
		} else {
			return event.createFollowup("Could not recognize GameDayChannel.");
		}
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
		SUB_COMMANDS.entrySet()
				.forEach(subCmd -> builder.addField(
					subCmd.getKey(), 
					subCmd.getValue().getDescription(), 
					false)
				);
		return builder.build();
	}
	
}
