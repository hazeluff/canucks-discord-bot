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
import com.hazeluff.discord.bot.ResourceLoader;
import com.hazeluff.discord.nhl.Game;
import com.hazeluff.discord.utils.Colors;
import com.hazeluff.discord.utils.wordcloud.Filters;
import com.hazeluff.discord.utils.wordcloud.Normalizers;
import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.bg.RectangleBackground;
import com.kennycason.kumo.font.KumoFont;
import com.kennycason.kumo.font.scale.FontScalar;
import com.kennycason.kumo.font.scale.LinearFontScalar;
import com.kennycason.kumo.nlp.FrequencyAnalyzer;
import com.kennycason.kumo.nlp.normalize.TrimToEmptyNormalizer;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Permission;

/**
 * Displays information about NHLBot and the author
 */
public class WordcloudCommand extends Command {
	private static final int MAX_WORDS = 2000;

	public WordcloudCommand(NHLBot nhlBot) {
		super(nhlBot);
	}

	public String getName() {
		return "wordcloud";
	}

	public ApplicationCommandRequest getACR() {
		return null;
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

		Game game = nhlBot.getGameScheduler().getGameByChannelName(channel.getName());
		if (game == null) {
			return;
		}

		ZoneId timeZone = nhlBot.getPersistentData().getPreferencesData()
				.getGuildPreferences(guild.getId().asLong()).getTimeZone();
		
		if(command.getArguments().isEmpty()) {
			sendWordcloud(channel, game, timeZone);
		} else {
			sendWordcloud(channel, game, timeZone,
					new LinearFontScalar(
							Integer.parseInt(command.getArguments().get(0)),
							Integer.parseInt(command.getArguments().get(1))
					)
			);
		}
	}

	public void sendWordcloud(TextChannel channel, Game game, ZoneId timeZone) {
		sendWordcloud(channel, game, timeZone, new LinearFontScalar(20, 140));
	}

	/**
	 * Generates a wordcloud for the given channel and submits it to #wordcloud.
	 * 
	 * @param timeZone
	 * @param channel
	 *            channel to analyze
	 * @param title
	 * @param fontScaler
	 */
	public void sendWordcloud(TextChannel channel, Game game, ZoneId timeZone, FontScalar fontScaler) {
		String title = GameDayChannel.getDetailsMessage(game, timeZone);
		new Thread(() -> {
			Message generatingMessage = nhlBot.getDiscordManager()
					.sendAndGetMessage(channel, "Generating Wordcloud for: " + title);

			List<String> messages = nhlBot.getDiscordManager()
					.block(channel.getMessagesBefore(generatingMessage.getId()).map(Message::getContent));

			Guild guild = nhlBot.getDiscordManager().block(channel.getGuild());
			sendMessage(nhlBot.getWordcloudChannelManager().get(guild), getReply(title, messages, fontScaler));
		}).start();
	}
	
	private Consumer<MessageCreateSpec> getReply(String title, List<String> messages, FontScalar fontScaler) {
		
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
		wordCloud.setFontScalar(fontScaler);
		wordCloud.setKumoFont(new KumoFont(ResourceLoader.get().getComicSansMSFont().getStream()));
		wordCloud.build(wordFrequencies);
		ByteArrayOutputStream wordcloudStream = new ByteArrayOutputStream();
		wordCloud.writeToStreamAsPNG(wordcloudStream);

		// Create Message
		String fileName = "wordcloud.png";
		InputStream fileStream = new ByteArrayInputStream(wordcloudStream.toByteArray());
		String message = title + String.format(". Total Messages: %s", messages.size());
		return spec -> spec.addFile(fileName, fileStream).setContent(message);
	}

	@Override
	public boolean isAccept(Message message, CommandArguments command) {
		return command.getCommand().equalsIgnoreCase(getName());
	}
}
