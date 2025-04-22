package com.hazeluff.discord.bot.command;

import java.util.List;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.nhl.Team;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Displays help for the NHLBot commands
 */
public class HelpCommand extends Command {
	static final String NAME = "help";

	public HelpCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	public String getName() {
		return NAME;
	}

	public ApplicationCommandRequest getACR() {
		return ApplicationCommandRequest.builder()
				.name(getName())
				.description("Get help on using " + Config.APPLICATION_NAME + ". Lists available commands")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("command")
                        .description("Name of the command you want help with. e.g. 'schedule'.")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(false)
                        .build())
				.build();
	}

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {
		String command = getOptionAsString(event, "command");
		if (command == null) {
			return event.reply(COMMAND_LIST);
		}
		switch(command) {
		case "teams":
			return reply(event, TEAMS_MESSAGE, true);
		case ScheduleCommand.NAME:
			return reply(event, ScheduleCommand.HELP_MESSAGE, true);
		case GDCCommand.NAME:
			return reply(event, GDCCommand.HELP_MESSAGE_EMBED, true);
		case SubscribeCommand.NAME:
			return reply(event, SubscribeCommand.HELP_MESSAGE, true);
		case UnsubscribeCommand.NAME:
			return reply(event, UnsubscribeCommand.HELP_MESSAGE, true);
		default:
			return reply(event, "Unknown command: " + command, true);
		}
	}

	public static final String COMMAND_LIST = "Here are a list of commands:\n\n"
			+ "You can use the commands by doing slash commands integrated with Discord.\n"
			+ "```\n" 
			+ "Setup\n"
			+ "  Creates/Removes game day channels for teams subscribed to. Team option should be provided with the three letter code of your team.\n\n"
			+ "  `subscribe team:`   - Subscribes you to a team.\n"
			+ "  `unsubscribe team:` - Unsubscribes you from a team.\n\n"

			+ "Game Day Channel\n"
			+ "  `gdc subcommand:`   - Game Day Channel commands. Must be used in Game Day Channels.`subcommand: score, goals, status`.\n\n"

			+ "General\n"
			+ "  `schedule`          - Displays information about the most recent and coming up games of your subscribed teams.\n"
			+ "  `about`             - Displays information about me."
			+ "```\n"
			+ "Join the support/demo server for help! https://discord.gg/vFV6DHcz";

	static final String TEAMS_MESSAGE = "Here are your teams:\n" + listOfTeams();

	public static String listOfTeams() {
		StringBuilder response = new StringBuilder("```");
		List<Team> teams = Team.getSortedNHLValues();
		for (Team team : teams) {
			response.append("\n").append(team.getCode()).append(" - ").append(team.getFullName());
		}
		response.append("```");
		return response.toString();

	}
}
