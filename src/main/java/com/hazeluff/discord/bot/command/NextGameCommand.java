package com.hazeluff.discord.bot.command;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.hazeluff.discord.bot.GameDayChannel;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.nhl.Game;
import com.hazeluff.discord.nhl.Team;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;

/**
 * Displays information about the next game.
 */
public class NextGameCommand extends Command {
	static final Consumer<MessageCreateSpec> NO_NEXT_GAME_MESSAGE = spec -> spec
			.setContent("There may not be a next game.");
	static final Consumer<MessageCreateSpec> NO_NEXT_GAMES_MESSAGE = spec -> spec
			.setContent("There may not be any games for any of your subscribed teams.");

	public NextGameCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	@Override
	public void execute(MessageCreateEvent event, CommandArguments command) {
		GuildPreferences preferences = nhlBot.getPersistentData()
				.getPreferencesData()
				.getGuildPreferences(event.getGuildId().get().asLong());
		List<Team> preferredTeams = preferences.getTeams();
		
		if (preferredTeams.isEmpty()) {
			sendMessage(event, SUBSCRIBE_FIRST_MESSAGE);
			return;
		}

		if (preferredTeams.size() == 1) {
			Game nextGame = nhlBot.getGameScheduler().getNextGame(preferredTeams.get(0));
			if(nextGame == null) {
				sendMessage(event, NO_NEXT_GAME_MESSAGE);
				return;
			}

			sendMessage(event, getNextGameDetailsMessage(nextGame, preferences));
			return;
		}

		Set<Game> games = preferredTeams.stream().map(team -> nhlBot.getGameScheduler().getNextGame(team))
				.filter(Objects::nonNull).collect(Collectors.toSet());
		if (games.isEmpty()) {
			sendMessage(event, NO_NEXT_GAMES_MESSAGE);
			return;
		}

		sendMessage(event, getNextGameDetailsMessage(games, preferences));
	}

	Consumer<MessageCreateSpec> getNextGameDetailsMessage(Game game, GuildPreferences preferences) {
		return spec -> spec.setContent(
				"The next game is:\n" + GameDayChannel.getDetailsMessage(game, preferences.getTimeZone()));
	}

	Consumer<MessageCreateSpec> getNextGameDetailsMessage(Set<Game> games, GuildPreferences preferences) {
		StringBuilder replyMessage = new StringBuilder("The following game(s) are upcomming:");
		for (Game game : games) {
			replyMessage.append("\n" + GameDayChannel.getDetailsMessage(game, preferences.getTimeZone()));
		}
		return spec -> spec.setContent(replyMessage.toString());
	}

	@Override
	public boolean isAccept(Message message, CommandArguments command) {
		return command.getCommand().equalsIgnoreCase("nextgame");
	}

}
