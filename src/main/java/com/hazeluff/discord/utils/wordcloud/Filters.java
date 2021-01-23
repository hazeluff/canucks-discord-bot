package com.hazeluff.discord.utils.wordcloud;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.kennycason.kumo.nlp.filter.Filter;

public class Filters {

	public static class UserTags extends Filter {

		@Override
		public boolean test(String word) {
			return !word.matches("<@!?[0-9]+>");
		}

	}

	/*
	 * Unused as OnlyAllowedChars will filter out URLs ('.', '/', ':' are not allowed)
	 */
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
	
	public static class Stopwords extends Filter {
		private static final Set<String> STOPWORDS = 
				Stream.of(
						"i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yours",
						"yourself", "yourselves", "he", "him", "his", "himself", "she", "her", "hers", "herself", "it",
						"its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who",
						"whom", "this", "that", "these", "those", "am", "is", "are", "was", "were", "be", "been",
						"being", "have", "has", "had", "having", "do", "does", "did", "doing", "a", "an", "the", "and",
						"but", "if", "or", "because", "as", "until", "while", "of", "at", "by", "for", "with", "about",
						"against", "between", "into", "through", "during", "before", "after", "above", "below", "to",
						"from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then",
						"once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each", "few",
						"more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so",
						"than", "too", "very", "s", "t", "can", "will", "just", "don", "should", "now"
				)
				.collect(Collectors.toCollection(HashSet::new));

		@Override
		public boolean test(String word) {
			return !STOPWORDS.contains(word);
		}

	}
}
