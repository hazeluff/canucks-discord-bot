package com.hazeluff.discord.bot.gdc.custom.game;

import com.hazeluff.nhl.Team;

@SuppressWarnings("serial")
public class CanucksGameCollection extends CustomGameMessage.Collection {
	public CanucksGameCollection() {
		super();
		win("https://cdn.discordapp.com/attachments/1170096185034412114/1171226441891971143/fuckyoupodium.png"); // Podium guy giving the birds
		// win("https://cdn.discordapp.com/attachments/1170096185034412114/1171226212769730620/vancouver-canucks-win.gif"); // Canucks Graphic Win
		// win("https://cdn.discordapp.com/attachments/1170096185034412114/1171226213164011520/sprite_win.gif"); // Canucks Graphic Win - Sprite
		// win("https://cdn.discordapp.com/attachments/1170096185034412114/1171226213545680896/canucks-win-vancouver-canucks.gif"); // Canucks Graphic Win
		// win("https://cdn.discordapp.com/attachments/1170096185034412114/1171226213931552798/fin_dance.gif"); // Fin Dance
		// win("https://cdn.discordapp.com/attachments/1170096185034412114/1171226214380359690/kass_cheer.gif"); // Kass Cheer
		
		// shutout("https://cdn.discordapp.com/attachments/1170096185034412114/1171223035072762058/shutout_231027.gif"); // Demko
		// shutout("https://cdn.discordapp.com/attachments/1170096185034412114/1171223035953545216/shutout_231104.gif"); // Demko
		lose("https://cdn.discordapp.com/attachments/1170084611422949396/1171226314477424820/petey_shake_head.gif"); // Petey shake head
		lose("https://cdn.discordapp.com/attachments/1170084611422949396/1171226313890214019/green_fuck_off.gif"); // Green Fuck Off
	}

	static CustomGameMessage win(String message) {
		return CustomGameMessage.win(message, Team.VANCOUVER_CANUCKS);
	}

	static CustomGameMessage lose(String message) {
		return CustomGameMessage.lose(message, Team.VANCOUVER_CANUCKS);
	}

	static CustomGameMessage shutout(String message) {
		return CustomGameMessage.shutout(message, Team.VANCOUVER_CANUCKS);
	}
}
