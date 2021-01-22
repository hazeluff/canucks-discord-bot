package com.hazeluff.discord.utils;

import java.awt.Color;

import com.kennycason.kumo.palette.ColorPalette;

public class Colors {
	public static class Main {
		public static final Color BLUE = new Color(0, 32, 91);
		public static final Color GREEN = new Color(10, 134, 61);
		public static final Color DARK_BLUE = new Color(4, 28, 44);
		public static final Color GRAY = new Color(153, 153, 154);
		public static final Color WHITE = new Color(255, 255, 255);
		
		public static final ColorPalette WORDCLOUD_PALLETE = new ColorPalette(BLUE, GREEN, WHITE);
		public static final Color WORDCLOUD_BACKGROUND = GRAY;
	}
}
