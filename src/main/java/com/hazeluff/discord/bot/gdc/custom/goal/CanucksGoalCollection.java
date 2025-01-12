package com.hazeluff.discord.bot.gdc.custom.goal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.hazeluff.nhl.Team;

@SuppressWarnings("serial")
public class CanucksGoalCollection extends CustomGoalMessage.Collection {
	Team getTeam() {
		return Team.VANCOUVER_CANUCKS;
	}

	public CanucksGoalCollection() {
		super();

		/*
		 * Players
		 */
		Map<Integer, List<String>> playerGoalmessages = new HashMap<>(); // Used to shorten invocation/copy-paste
		
		// Elias Pettersson
		playerGoalmessages.put(8480012, Arrays.asList(
			"Pistol Pete! 🔫",
			"Petey!",
			"https://tenor.com/view/im-watching-you-state-pettersson-nhl-canucks-gif-13968152", // Pointing Eyes
			"https://tenor.com/view/omg-nhl-canucks-pettersson-oh-my-god-gif-13968150", // OMG
			"https://tenor.com/view/pettersson-canucks-gif-23371660", 
			"https://www.reddit.com/r/canucks/comments/km7poe/suck_it_bitch/", // Suck it bitch
			"https://tenor.com/view/pettersson-reaction-nhl-goal-canucks-gif-12739274",
			"https://tenor.com/view/vancouver-canucks-elias-pettersson-canucks-nhl-hockey-gif-18749376", // Smile; Look up
			"https://giphy.com/gifs/nhl-reaction-react-elias-pettersson-fXV8I6OjWDfwH3IFIy", // Oooh, not bad
			"https://giphy.com/gifs/nhl-pettersson-petey-elias-QYN1eeGNi9YSQp6cAp", // Thumbs up
			"https://giphy.com/gifs/nhl-pettersson-petey-elias-WSxwsOYnc9SW9vK3JS", // ¯\_(ツ)_/¯
			"https://giphy.com/gifs/twitter-hockey-hockeytwitter-twitter-8AgIDszVn2akqTD6tO", // Bow
			"https://giphy.com/gifs/nhl-pettersson-petey-elias-UtJJs9AOyIGtIIWKk4", // Dust shoulder
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791505745776690/petey_goal_2024.gif", // Canucks 2024
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791510376419370/petey_goal_skate_2024.gif" // Canucks 2024 - Skate
		));

		
		hatTrick("https://giphy.com/gifs/nhl-canucks-vancouver-rookie-of-the-year-eMt84wERIUWJgw8T1w", 8480012); // Calder Award
		goals("https://cdn.discordapp.com/attachments/1170084611422949396/1171226312543850586/2018_mr_petey_sr.gif", 3, 8480012, 2); // Dad - 2 fingers
		
		// Brock Boeser
		playerGoalmessages.put(8478444, Arrays.asList(
			"Brock Hard",
			"https://tenor.com/view/vancouver-canucks-brock-boeser-nhl-hockey-canucks-gif-16393197", // Allstar
			"https://giphy.com/gifs/nhl-goal-celly-brock-boeser-5brOrJAyCJpD55jOet", // Woooo
			"https://giphy.com/gifs/nhl-hockey-ice-yr44xcG3kxcylLrNja", // Double pump
			"https://giphy.com/gifs/nhl-sports-hockey-ice-2kdTpPuGUEVLSulQw0", // Woooo
			"https://tenor.com/view/michael-buble-flow-brock-boeser-suave-gif-11485495", // Buble mirror
			"https://i.redd.it/wn69r1sgegf01.jpg", // The flow
			"https://giphy.com/gifs/canucks-brock-boeser-goal-2021-mn5mhmP5rNE9y8sxtC", // Canucks Graphic 2021
			"https://cdn.discordapp.com/attachments/276953120964083713/1167941191883563089/boeser_2023.gif", // Canucks Graphic 2023
			"https://media.discordapp.net/attachments/1159191596647075843/1171177026485501973/brockboeserlookup.gif" // Canucks 2023 - Black Skate Promo
		));
		
		hatTrick("https://www.youtube.com/watch?v=vjheiAQbhQw", 8478444); // Boeser....SCORES
		hatTrick("https://cdn.discordapp.com/attachments/1170625431741935677/1171293394589466704/Boeser.MOV", 8478444); // BOOOESSERRRRRRR
		hatTrick("https://cdn.discordapp.com/attachments/1170084611422949396/1220227160254451713/boeser_petey_cele_smile.gif", 8478444); // Brock cele smile 2023

		// J.T. Miller
		playerGoalmessages.put(8478444, Arrays.asList(
			"**JONATHAN TONATHAN!**",
			"**It's Miller Time!**",
			"https://giphy.com/gifs/canucks-jt-miller-goal-2021-62XQNbTolaM9zokRAP", // Canucks Graphic 2021
			"https://cdn.discordapp.com/attachments/276953120964083713/1167944428615368826/jt_2023.gif", // Canucks Graphic 2023
			"https://cdn.discordapp.com/attachments/1170084611422949396/1170085862114078780/miller_cele.gif", // Cele
			"https://cdn.discordapp.com/attachments/1170084611422949396/1170094295953461381/miller_goal_skate_23.gif", // Canucks Graphic 2023 - Skate
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791526532878418/miller_goal_2024.gif", // Canucks 2024
			"https://cdn.discordapp.com/attachments/1170084611422949396/1170094295953461381/miller_goal_skate_23.gif" // Canucks 2024 - Skate
		));

		// Nils Hoglander
		playerGoalmessages.put(8481535, Arrays.asList(
			"https://tenor.com/view/lion-king-pumba-hunting-bugs-gif-8591753",
			"https://giphy.com/gifs/nhl-nhl-prospect-2019-scouting-combine-nils-hoglander-H1HFy3uN2FNSb7jRiv", // Draft; Sign camera
			"https://giphy.com/gifs/nhl-sports-hockey-ice-1tNHEPNQwnUbNjfIJz", // Wooo
			"https://giphy.com/gifs/canucks-nils-hoglander-goal-2021-PPUGEWg40luejKqx8Z", // Canucks Graphic 2021
			"https://cdn.discordapp.com/attachments/1170084611422949396/1220212315287195788/hoggy_skate_goal_2023.gif", // Canucks Graphic 2023
			"https://cdn.discordapp.com/attachments/1170084611422949396/1220227155909410878/clash-of-clans-hog-rider.gif", // Clash rider
			"https://cdn.discordapp.com/attachments/1170084611422949396/1220227160820809728/hog-hulk-power-up.gif", // Hulk Hogan
			"https://tenor.com/view/razorback-hog-pig-riding-wild-pig-running-gif-17002611"				
		));
	

		// Connor Garland
		playerGoalmessages.put(8478856, Arrays.asList(
			"https://i.redd.it/b6x0grvcxbv71.png", // Death Stare
			"https://giphy.com/gifs/canucks-conor-garland-goal-2021-ZBOuVtS3f3WoZNFEJu", // Canucks Graphic 2021
			"https://tenor.com/view/vancouver-canucks-conor-garland-running-fast-skating-speed-gif-24103563", // Skating at camera
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791803445022740/garly_goal_2021.gif", // Canucks Graphic 2021
			"https://cdn.discordapp.com/attachments/276953120964083713/1167950362905411655/garland_2023.gif", // Canucks Graphic 2023
			"https://cdn.discordapp.com/attachments/1170084611422949396/1170085860117586061/garland_cele.gif", // Cele
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791528353071154/garly_goal_2024.gif", // Canucks 2024
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791523076505641/garly_goal_skate_2024.gif" // Canucks 2024 - Skate
		));

		// Quinn Hughes
		playerGoalmessages.put(8480800, Arrays.asList(
			"**Huggy Bear!** 🧸",
			"https://tenor.com/view/vancouver-canucks-quinn-hughes-canucks-canucks-goal-canucks-win-gif-18795481", // Fist bump bench
			"https://giphy.com/gifs/canucks-quinn-hughes-goal-2021-canucks-twJhUrLKMTCL4kxrfw", // Canucks Graphic 2021
			"https://cdn.discordapp.com/attachments/276953120964083713/1167942592412012615/huggy_2023.gif", // Canucks Graphic 2023
			"https://cdn.discordapp.com/attachments/1170084611422949396/1170085860679614606/huggy_cele.gif", // Cele
			"https://cdn.discordapp.com/attachments/1170084611422949396/1170094295328493568/huggy_goal_skate_23.gif", // Canucks Graphic 2023 - Skate
			"https://media.discordapp.net/attachments/1159191596647075843/1171173832267137094/quinnhughespoint.gif", // Canucks 2023 - Black Skate Promo
			"https://cdn.discordapp.com/attachments/1170084611422949396/1172771154281373798/hughes_skate_goal_ingame.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791631373697165/huggy_goal_2024.gif", // Canucks 2024
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791522459947108/huggy_goal_skate_2024.gif" // Canucks 2024 - Skate
		));
		
		// Tyler Myers
		playerGoalmessages.put(8474574, Arrays.asList(
			"🦒",
			"https://cdn.discordapp.com/emojis/685700452826087457.png",
			"https://tenor.com/view/toys-r-us-geoffrey-the-giraffe-dancing-dance-dance-off-gif-16386148", // ToysRUs Geofrey
			"https://cdn.discordapp.com/attachments/276953120964083713/1167951778059386900/myers_2023.gif" // Canucks Graphic 2023
		));

		// Phil DiGiuseppe
		playerGoalmessages.put(8476858, Arrays.asList(
			"https://cdn.discordapp.com/attachments/276953120964083713/1167713608231297064/ogrbsdx0bvwb1.png",
			"https://tenor.com/view/vancouver-canucks-phillip-di-giuseppe-canucks-canucks-goal-canucks-win-gif-6680089124421974858",
			"https://cdn.discordapp.com/attachments/276953120964083713/1167950366021799966/pdg_2023.gif"
		));

		// Dakota Joshua
		playerGoalmessages.put(8478057, Arrays.asList(
			"https://cdn.discordapp.com/attachments/276953120964083713/1167950361735209040/joshua_2023.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1220216649958035516/dakota_skate_goal_ingame_2023.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791521616887888/dak_goal_2024.gif", // Canucks 2024
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791525169729536/dak_goal_skate_2024.gif" // Canucks 2024 - Skate
		));

		// Carson Soucy
		playerGoalmessages.put(8477369, Arrays.asList(
			"https://cdn.discordapp.com/attachments/276953120964083713/1167950364193083453/soucy_2023.gif"
		));

		// Pius Suter
		playerGoalmessages.put(8480459, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1170087547918102609/suter_goal.gif",
			"https://media.discordapp.net/attachments/1184407430159945768/1184967743821328444/GBVi3PSaoAAkXcr.png", // Pew Pew
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791508081999946/suter_goal_2024.gif", // Canucks 2024
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791523839868992/suter_goal_skate_2024.gif" // Canucks 2024 - Skate
		));

		// Teddy Blueger
		playerGoalmessages.put(8476927, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1220206508990664754/blueger.gif", // Canucks 2023
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791629364494397/blueger_goal_2024.gif" // Canucks 2024
		));

		// Filip Hronek
		playerGoalmessages.put(8479425, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1220227157033484288/hronek_2023.gif" // Canucks 2023
		));

		// Thatcher Demko
		playerGoalmessages.put(8477967, Arrays.asList(
			"https://cdn.discordapp.com/attachments/240245066017406976/1171178501886447657/demkohattilt.gif"
		));
		
		// Jake DeBrusk
		playerGoalmessages.put(8478498, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791632560422912/debrusk_cele.gif", // Cele
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791509000421407/debrusk_goal_2024.gif", // Canucks 2024
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791525857460325/debrusk_goal_skate_2024.gif" // Canucks 2024 - Skate
		));
		
		// Kiefer Sherwood
		playerGoalmessages.put(8480748, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791506551210160/sherwood_goal_2024.gif", // Canucks 2024
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791509667577979/sherwood_goal_skate_2024.gif" // Canucks 2024 - Skate
		));

		// Raty
		playerGoalmessages.put(8482691, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791507331354686/raty_goal_2024.gif" // Canucks 2024
		));

		// Arshdeep Bains
		playerGoalmessages.put(8483395, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791627086987357/bains_goal_skate_2024.gif" // Canucks 2024 - Skate
		));
		
		
		// register player goal messages
		for (Entry<Integer, List<String>> playerGoalMessage : playerGoalmessages.entrySet()) {
			int scorer = playerGoalMessage.getKey();
			List<String> messages = playerGoalMessage.getValue();

			for (String message : messages) {
				scorer(message, scorer);
			}
		}
		
		/*
		 *  Combos
		 */
		involved("LOTTO 6 - 40 - 9 🎫", 
				8478444, 8480012, 8476468); // Brock, Petey, JT
		
		// Brock, Petey
		involved("https://tenor.com/view/brock-boeser-vancouver-canucks-canucks-canucks-goal-canucks-win-gif-18115227", 
				8478444, 8480012); // Goal celly hug
		involved("https://cdn.discordapp.com/attachments/1170084611422949396/1171233918159163432/fucked_cut.gif", 
				8478444, 8480012); // Fucked
		involved("https://giphy.com/gifs/nhl-goal-hug-pettersson-elias-OqAeJeUnA1S9n4XIk2", 
				8478444, 8480012); // Petey goal celly
		involved("https://giphy.com/gifs/nhl-vancouver-canucks-elias-pettersson-brock-boeser-XGUFWnjdpfU7y2DEB5",
				8478444, 8480012); // Bench; Brock arm around Petey
		involved("https://media.discordapp.net/attachments/1170084611422949396/1171226315089776761/2018_brock_petey.gif",
				8478444, 8480012); // Bench; Pat Knee
		

		// Huggy, Hronek
		involved("https://cdn.discordapp.com/attachments/276953120964083713/1167937724649590918/batman_and_robin.mp4",
				8480800, 8479425);

		// Dak, Garly
		involved("https://cdn.discordapp.com/attachments/1170084611422949396/1327791628655788042/dak_gar_tims.jpg",
				8478057, 8478856);

		/*
		 * Team
		 */
		team("https://tenor.com/view/vancouver-canucks-fin-the-whale-canucks-nhl-mascot-gif-16319829"); // Fin
		team("https://tenor.com/view/vancouver-canucks-fin-the-whale-goal-canucks-goal-go-canucks-go-gif-16515260"); // Fin ref
		team("https://tenor.com/view/vancouver-canucks-fin-the-whale-canucks-lets-go-canucks-go-canucks-go-gif-16279068"); // Fin flying v dance
		team("https://tenor.com/view/vancouver-canucks-gif-18155387"); // Fin flying v dance
		team("https://tenor.com/view/vancouver-canucks-nhl-hockey-canucks-win-dancing-gif-16364902"); // The clapper
		team("https://tenor.com/view/vancouver-canucks-canucks-goal-canucks-win-goal-score-gif-16300297"); // Generic
		team("https://giphy.com/gifs/canucks-vancouver-canucks-goal-VZidxu7DtmXARTVxgF"); // Generic
		team("https://giphy.com/gifs/goal-zack-kassian-p0vOT3eYQKAFO"); // Kass; Fuck Yeah
		team("https://www.youtube.com/watch?v=z9WeBV8O3ag"); // 2019 horn - Holiday
		team("https://www.youtube.com/watch?v=Jv7wN9a3u4M"); // 2020 horn - Ain't talking bout love
		team("https://giphy.com/gifs/nhl-cute-baby-fan-MWdNeUQqWhe3k4jMX0"); // Baby wave
		team("https://gfycat.com/memorablehollowamericancicada-green-dance-man"); // Green men
		team("https://cdn.discordapp.com/attachments/276953120964083713/1167950362293063781/2023.gif"); // Generic 2023
		team("https://cdn.discordapp.com/attachments/1170084611422949396/1170085859450695810/ea_fin.gif"); // EA Fin
		team("https://cdn.discordapp.com/attachments/1170084611422949396/1171226313307197450/2018_fan_pound_chest.gif"); // Fan Chest Pound
		team("https://cdn.discordapp.com/attachments/1170084611422949396/1171226315970596915/2018_kid.gif"); // Fan Kid Impressed
		team("https://cdn.discordapp.com/attachments/1170084611422949396/1220227159772364821/canucks_skate_christmas_2023.gif"); // Christmas Skate 2023
	}
}
