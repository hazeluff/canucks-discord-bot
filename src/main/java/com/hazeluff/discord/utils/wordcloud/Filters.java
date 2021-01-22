package com.hazeluff.discord.utils.wordcloud;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

import com.kennycason.kumo.nlp.filter.Filter;

public class Filters {

	public static class UserTags extends Filter {

		@Override
		public boolean test(String word) {
			return !word.matches("<@!?[0-9]+>");
		}

	}

	public static class Urls extends Filter {

		@Override
		public boolean test(String word) {
			try {
				URL obj = new URL(word);
				obj.toURI();
				return false;
			} catch (MalformedURLException e) {
				return true;
			} catch (URISyntaxException e) {
				return true;
			}
		}

	}

	public static class Commands extends Filter {

		@Override
		public boolean test(String word) {
			return !word.startsWith("?");
		}

	}

	public static class OnlyAllowedChars extends Filter {
		private static final Pattern pattern = Pattern.compile("[^a-zA-Z0-9#']+");

		@Override
		public boolean test(String word) {
			return !pattern.matcher(word).find();
		}
	}
}
