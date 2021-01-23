package com.hazeluff.discord.bot.command;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.hazeluff.discord.bot.GameDayChannel;
import com.hazeluff.discord.bot.NHLBot;
import com.hazeluff.discord.nhl.Game;
import com.hazeluff.discord.utils.Colors;
import com.hazeluff.discord.utils.wordcloud.Filters;
import com.hazeluff.discord.utils.wordcloud.Normalizers;
import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.bg.RectangleBackground;
import com.kennycason.kumo.font.scale.LinearFontScalar;
import com.kennycason.kumo.nlp.FrequencyAnalyzer;
import com.kennycason.kumo.nlp.normalize.TrimToEmptyNormalizer;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Permission;

/**
 * Displays information about NHLBot and the author
 */
public class WordcloudCommand extends Command {
	private static final int MAX_WORDS = 2000;

	public WordcloudCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	@Override
	public void execute(MessageCreateEvent event, CommandArguments command) {
		Guild guild = getGuild(event);
		TextChannel channel = getChannel(event);
		Message message = event.getMessage();
		Member user = getMessageAuthor(message);
		if (!isOwner(guild, user)
				&& !hasPermissions(guild, user, Arrays.asList(Permission.ADMINISTRATOR))) {
			return;
		}

		Game game = getNHLBot().getGameScheduler().getGameByChannelName(channel.getName());
		if (game == null) {
			return;
		}

		ZoneId timeZone = getNHLBot().getPersistentData().getPreferencesData()
				.getGuildPreferences(guild.getId().asLong()).getTimeZone();
		String title = "Wordcloud for " + GameDayChannel.getDetailsMessage(game, timeZone);
		sendWordcloud(timeZone, channel, title);
	}

	public void sendWordcloud(ZoneId timeZone, TextChannel channel, String title) {
		new Thread(() -> {
			Message generatingMessage = getNHLBot().getDiscordManager()
					.sendAndGetMessage(channel, "Generating Wordcloud...");

			List<String> messages = getNHLBot().getDiscordManager()
					.block(channel.getMessagesBefore(generatingMessage.getId()).map(Message::getContent));

			sendMessage(channel, getReply(title, messages));
		}).start();
	}

	public Consumer<MessageCreateSpec> getReply(String title, List<String> messages) {
		return getReply(title, messages, 20, 200);
	}
	
	public Consumer<MessageCreateSpec> getReply(String title, List<String> messages, int minFont, int maxFont) {
		
		// Create WordCloud
		final FrequencyAnalyzer frequencyAnalyzer = new FrequencyAnalyzer();
		frequencyAnalyzer.setNormalizer(new TrimToEmptyNormalizer());
		frequencyAnalyzer.addNormalizer(new Normalizers.EmoteNormalizer());
		frequencyAnalyzer.setMinWordLength(2);
		frequencyAnalyzer.addFilter(new Filters.OnlyAllowedChars());
		frequencyAnalyzer.addFilter(new Filters.Stopwords());
		frequencyAnalyzer.addFilter(new Filters.Commands());
		frequencyAnalyzer.addFilter(new Filters.UserTags());
		frequencyAnalyzer.setWordFrequenciesToReturn(MAX_WORDS);
		List<WordFrequency> wordFrequencies = frequencyAnalyzer.load(messages);
		
		
		final Dimension dimension = new Dimension(800, 600);
		final WordCloud wordCloud = new WordCloud(dimension, CollisionMode.PIXEL_PERFECT);
		wordCloud.setPadding(0);
		wordCloud.setBackground(new RectangleBackground(dimension));
		wordCloud.setBackgroundColor(Colors.Main.WORDCLOUD_BACKGROUND);
		wordCloud.setColorPalette(Colors.Main.WORDCLOUD_PALLETE);
		wordCloud.setFontScalar(new LinearFontScalar(minFont, maxFont));
		wordCloud.build(wordFrequencies);
		ByteArrayOutputStream wordcloudStream = new ByteArrayOutputStream();
		wordCloud.writeToStreamAsPNG(wordcloudStream);

		// Create Message
		String fileName = "wordcloud.png";
		InputStream fileStream = new ByteArrayInputStream(wordcloudStream.toByteArray());
		return spec -> spec.addFile(fileName, fileStream).setContent(title);
	}

	@Override
	public boolean isAccept(Message message, CommandArguments command) {
		return command.getCommand().equalsIgnoreCase("wordcloud");
	}
}
