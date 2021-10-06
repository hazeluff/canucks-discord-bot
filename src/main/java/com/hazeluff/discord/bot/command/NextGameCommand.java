package com.hazeluff.discord.bot.command;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.GameDayChannel;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.nhl.Game;
import com.hazeluff.nhl.Team;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;

/**
 * Displays information about the next game.
 */
public class NextGameCommand extends Command {
	static final String NAME = "nextgame";

	public NextGameCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	public String getName() {
		return NAME;
	}

	public ApplicationCommandRequest getACR() {
		return ApplicationCommandRequest.builder()
				.name(getName())
				.description("Get the next game for your subscribed (or defined) team.")
				.build();
	}

	@Override
	public Publisher<?> onChatCommandInput(ChatInputInteractionEvent event) {
		Snowflake guildId = event.getInteraction().getGuildId().get();
		GuildPreferences preferences = nhlBot.getPersistentData().getPreferencesData()
				.getGuildPreferences(guildId.asLong());
		List<Team> preferredTeams = preferences.getTeams();

		if (preferredTeams.isEmpty()) {
			return event.replyEphemeral(SUBSCRIBE_FIRST_MESSAGE);
		}

		if (preferredTeams.size() == 1) {
			Game nextGame = nhlBot.getGameScheduler().getNextGame(preferredTeams.get(0));
			if (nextGame == null) {
				return event.replyEphemeral(NO_NEXT_GAME_MESSAGE);
			}
			return event.replyEphemeral(getNextGameDetailsMessage(nextGame, preferences));
		}

		Set<Game> games = preferredTeams.stream().map(team -> nhlBot.getGameScheduler().getNextGame(team))
				.filter(Objects::nonNull).collect(Collectors.toSet());
		if (games.isEmpty()) {
			return event.replyEphemeral(NO_NEXT_GAMES_MESSAGE);
		}

		return event.replyEphemeral(getNextGameDetailsMessage(games, preferences));
	}

	static final String NO_NEXT_GAME_MESSAGE = "There may not be a next game.";
	static final String NO_NEXT_GAMES_MESSAGE = "There may not be any games for any of your subscribed teams.";

	String getNextGameDetailsMessage(Game game, GuildPreferences preferences) {
		return "The next game is:\n" + GameDayChannel.getDetailsMessage(game, preferences.getTimeZone());
	}

	String getNextGameDetailsMessage(Set<Game> games, GuildPreferences preferences) {
		StringBuilder replyMessage = new StringBuilder("The following game(s) are upcomming:");
		for (Game game : games) {
			replyMessage.append("\n" + GameDayChannel.getDetailsMessage(game, preferences.getTimeZone()));
		}
		return replyMessage.toString();
	}

}
