package com.hazeluff.discord.bot.command;

import java.util.function.Consumer;

import com.hazeluff.discord.Config;
import com.hazeluff.discord.bot.NHLBot;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Displays help for the NHLBot commands
 */
public class HelpCommand extends Command {

	public HelpCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	public String getName() {
		return "help";
	}

	public ApplicationCommandRequest getACR() {
		return ApplicationCommandRequest.builder()
				.name(getName())
				.description("Get help on using " + Config.APPLICATION_NAME)
				.build();
	}

	@Override
	public void execute(MessageCreateEvent event, CommandArguments command) {
		sendMessage(event, getReply());
	}

	public Consumer<MessageCreateSpec> getReply() {
		return HELP_REPLY;
	}

	private static final Consumer<MessageCreateSpec> HELP_REPLY = spec -> spec.setContent(
			"Here are a list of commands:\n\n"
			+ "You can use the commands by doing `?canucksbot [command]` or `?[command]`.\n\n"

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

			+ "Commands with **(+)** have detailed help and can be accessed by typing:\n"
			+ "`?canucksbot [command] help`");

	@Override
	public boolean isAccept(Message message, CommandArguments command) {
		return command.getCommand().equalsIgnoreCase("help");
	}

}
