package com.hazeluff.discord.bot.command;

import java.util.List;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.nhl.Team;

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
			return event.replyEphemeral(TEAMS_MESSAGE);
		case ScheduleCommand.NAME:
			return event.replyEphemeral(ScheduleCommand.HELP_MESSAGE);
		case SubscribeCommand.NAME:
			return event.replyEphemeral(SubscribeCommand.HELP_MESSAGE);
		case UnsubscribeCommand.NAME:
			return event.replyEphemeral(UnsubscribeCommand.HELP_MESSAGE);
		default:
			return event.replyEphemeral("Unknown command: " + command);
		}
	}

	public static final String COMMAND_LIST = "Here are a list of commands:\n\n"
			+ "You can use the commands by doing `/[command]`.\n\n"

			+ "`subscribe [team]` - Subscribes you to a team. "
			+ "[team] is the three letter code of your team. **(+)**\n"

			+ "`unsubscribe [team]` - Unsubscribes you from a team. **(+)**\n"

			+ "`schedule` - Displays information about the most recent and coming up games of your "
			+ "subscribed teams. **(+)**\n"

			+ "`score` - Displays the score of the game. "
			+ "You must be in a 'Game Day Channel' to use this command.\n"

			+ "`goals` - Displays the goals of the game. "
			+ "You must be in a 'Game Day Channel' to use this command.\n"

			+ "`about` - Displays information about me.\n\n"

			+ "Commands with **(+)** have detailed help and can be accessed by using:\n" + "`/help [command]`";

	static final String TEAMS_MESSAGE = "Here are your teams:\n" + listOfTeams();

	public static String listOfTeams() {
		StringBuilder response = new StringBuilder("```");
		List<Team> teams = Team.getSortedLValues();
		for (Team team : teams) {
			response.append("\n").append(team.getCode()).append(" - ").append(team.getFullName());
		}
		response.append("```");
		return response.toString();

	}
}
