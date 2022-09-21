package com.hazeluff.discord.bot.command;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;

import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.bot.database.preferences.GuildPreferences;
import com.hazeluff.discord.bot.gdc.GameDayChannel;
import com.hazeluff.nhl.Team;
import com.hazeluff.nhl.game.Game;

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
			return event.reply(SUBSCRIBE_FIRST_MESSAGE).withEphemeral(true);
		}

		if (preferredTeams.size() == 1) {
			Game nextGame = nhlBot.getGameScheduler().getNextGame(preferredTeams.get(0));
			if (nextGame == null) {
				return event.reply(NO_NEXT_GAME_MESSAGE).withEphemeral(true);
			}
			return event.reply(getNextGameDetailsMessage(nextGame, preferences)).withEphemeral(true);
		}

		Set<Game> games = preferredTeams.stream().map(team -> nhlBot.getGameScheduler().getNextGame(team))
				.filter(Objects::nonNull).collect(Collectors.toSet());
		if (games.isEmpty()) {
			return event.reply(NO_NEXT_GAMES_MESSAGE).withEphemeral(true);
		}

		return event.reply(getNextGameDetailsMessage(games, preferences)).withEphemeral(true);
	}

	static final String NO_NEXT_GAME_MESSAGE = "There may not be a next game.";
	static final String NO_NEXT_GAMES_MESSAGE = "There may not be any games for any of your subscribed teams.";

	String getNextGameDetailsMessage(Game game, GuildPreferences preferences) {
		return "The next game is:\n" + GameDayChannel.getDetailsMessage(game);
	}

	String getNextGameDetailsMessage(Set<Game> games, GuildPreferences preferences) {
		StringBuilder replyMessage = new StringBuilder("The following game(s) are upcomming:");
		for (Game game : games) {
			replyMessage.append("\n" + GameDayChannel.getDetailsMessage(game));
		}
		return replyMessage.toString();
	}

}
