package com.hazeluff.discord.utils.wordcloud;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.kennycason.kumo.nlp.normalize.Normalizer;

public class Normalizers {
	
	public static class EmoteNormalizer implements Normalizer {
		Pattern pattern = Pattern.compile("<a?:(.*):[0-9]+>");

		@Override
		public String apply(String word) {
			Matcher matcher = pattern.matcher(word);
			return matcher.find() ? matcher.group(1) : StringUtils.capitalize(word);
		}
		
	}
}
