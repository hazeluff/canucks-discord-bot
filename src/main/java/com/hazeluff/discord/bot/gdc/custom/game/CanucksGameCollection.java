package com.hazeluff.discord.bot.gdc.custom.game;

import com.hazeluff.discord.nhl.NHLTeams.Team;

@SuppressWarnings("serial")
public class CanucksGameCollection extends CustomGameMessage.Collection {
	Team getTeam() {
		return Team.VANCOUVER_CANUCKS;
	}
	
	public CanucksGameCollection() {
		super();

		// Win
		win("https://cdn.discordapp.com/attachments/1170096185034412114/1171226441891971143/fuckyoupodium.png"); // Podium guy giving the birds
		win("https://cdn.discordapp.com/attachments/1170096185034412114/1171226212769730620/vancouver-canucks-win.gif"); // Canucks Graphic Win
		win("https://cdn.discordapp.com/attachments/1170096185034412114/1171226213164011520/sprite_win.gif"); // Canucks Graphic Win - Sprite
		win("https://cdn.discordapp.com/attachments/1170084611422949396/1327791627963465758/canucks_win_2023.gif"); // Canucks Graphic Win 2023
		win("https://cdn.discordapp.com/attachments/1170096185034412114/1171226213931552798/fin_dance.gif"); // Fin Dance
		win("https://cdn.discordapp.com/attachments/1170096185034412114/1171226214380359690/kass_cheer.gif"); // Kass Cheer
		// win("https://cdn.discordapp.com/attachments/1170084611422949396/1220226960907829278/fin_penguins.gif"); // Fin eats penguin
		// win("https://cdn.discordapp.com/attachments/1170084611422949396/1220226961389912164/fin_preds.gif"); // Fin shoves preds
		// win("https://cdn.discordapp.com/attachments/1170084611422949396/1220226961901748234/fin_bruins.gif"); // Fin smelly bruin
		win("https://cdn.discordapp.com/attachments/1170084611422949396/1327791802010304552/win_241201_hugs.gif");
		win("https://cdn.discordapp.com/attachments/1170084611422949396/1327791511001104395/locker_cele_dak_gar.gif");
		win("https://cdn.discordapp.com/attachments/1170084611422949396/1327791633114206349/we_will_never_lose_again.mp4"); // Whale meme

		// Lose
		lose("https://cdn.discordapp.com/attachments/1170084611422949396/1171226314477424820/petey_shake_head.gif"); // Petey shake head
		lose("https://cdn.discordapp.com/attachments/1170084611422949396/1171226313890214019/green_fuck_off.gif"); // Green Fuck Off
		lose("https://media.discordapp.net/attachments/240245066017406976/1194484673238274068/20240109_193445.jpg"); // Tocc mindfuck 
		lose("https://media.discordapp.net/attachments/240245066017406976/1194484672877559899/20240109_193449.jpg"); // Tocc rub temples
		lose("https://cdn.discordapp.com/attachments/1170084611422949396/1327791802467745792/tocc_pat_head.gif"); // Tocc pats head

		// Performance Based
		mostGoalsOrPoints("https://cdn.discordapp.com/attachments/1170084611422949396/1220227156441956432/corolla.gif", 8478856); // Garland - You got Corolla'd
		mostGoalsOrPoints("https://cdn.discordapp.com/attachments/1170084611422949396/1327791504877551727/garly_knod.gif", 8478856); // Garly knod
		mostGoalsOrPoints("https://cdn.discordapp.com/attachments/1170084611422949396/1219060285755883601/GI5_Mi5aAAAxAdX.png", 8478444); // Brockstar
		mostGoalsOrPoints("https://media.discordapp.net/attachments/1170084611422949396/1327791803042103376/quinn_godfather.jpg", 8480800); // Godfather
		
		// Shutouts
		shutout("https://cdn.discordapp.com/attachments/1170096185034412114/1171223035072762058/shutout_231027.gif"); // Demko
		shutout("https://cdn.discordapp.com/attachments/1170096185034412114/1171223035953545216/shutout_231104.gif"); // Demko
		shutout("https://cdn.discordapp.com/attachments/240245066017406976/1171178501886447657/demkohattilt.gif"); // Demko hat tilt
		shutout("https://cdn.discordapp.com/attachments/1170084611422949396/1220265360029126687/IMG_6339.png"); // DeSmith Zad
		
	}
}
