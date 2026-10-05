package com.hazeluff.discord.bot.gdc.nhl.custom.goal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.hazeluff.discord.nhl.NHLTeams.Team;

@SuppressWarnings("serial")
public class CanucksGoalCollection extends CustomGoalMessage.Collection {
	@Override
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
			"https://cdn.discordapp.com/attachments/1170084611422949396/1556466518597042206/suckitbitch.mp4", // Suck it bitch
			"https://tenor.com/view/pettersson-reaction-nhl-goal-canucks-gif-12739274",
			"https://tenor.com/view/vancouver-canucks-elias-pettersson-canucks-nhl-hockey-gif-18749376", // Smile; Look up
			"https://giphy.com/gifs/nhl-reaction-react-elias-pettersson-fXV8I6OjWDfwH3IFIy", // Oooh, not bad
			"https://giphy.com/gifs/nhl-pettersson-petey-elias-QYN1eeGNi9YSQp6cAp", // Thumbs up
			"https://giphy.com/gifs/nhl-pettersson-petey-elias-WSxwsOYnc9SW9vK3JS", // ¯\_(ツ)_/¯
			"https://giphy.com/gifs/twitter-hockey-hockeytwitter-twitter-8AgIDszVn2akqTD6tO", // Bow
			"https://giphy.com/gifs/nhl-pettersson-petey-elias-UtJJs9AOyIGtIIWKk4", // Dust shoulder
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791505745776690/petey_goal_2024.gif", // Canucks 2024
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791510376419370/petey_goal_skate_2024.gif", // Canucks 2024 - Skate
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547347047776287/petey_skate_goal_2025.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547346653515936/petey_goal_2025.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1556471394823438376/petey_goal_26.gif"
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
			"https://media.discordapp.net/attachments/1159191596647075843/1171177026485501973/brockboeserlookup.gif", // Canucks 2023 - Black Skate Promo
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547828700414032/boeser_skate_goal_2025.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547828121866393/boeser_sign.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547827798642688/boeser_goal_celly_2025.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1556471263436742706/boeser_goal_26.gif"
		));
		
		hatTrick("https://www.youtube.com/watch?v=vjheiAQbhQw", 8478444); // Boeser....SCORES
		hatTrick("https://cdn.discordapp.com/attachments/1170625431741935677/1171293394589466704/Boeser.MOV", 8478444); // BOOOESSERRRRRRR
		hatTrick("https://cdn.discordapp.com/attachments/1170084611422949396/1220227160254451713/boeser_petey_cele_smile.gif", 8478444); // Brock cele smile 2023

		// Phil DiGiuseppe
		playerGoalmessages.put(8476858, Arrays.asList(
			"https://cdn.discordapp.com/attachments/276953120964083713/1167713608231297064/ogrbsdx0bvwb1.png",
			"https://tenor.com/view/vancouver-canucks-phillip-di-giuseppe-canucks-canucks-goal-canucks-win-gif-6680089124421974858",
			"https://cdn.discordapp.com/attachments/276953120964083713/1167950366021799966/pdg_2023.gif"
		));

		// Filip Hronek
		playerGoalmessages.put(8479425, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1220227157033484288/hronek_2023.gif", // Canucks
																												// 2023
			"https://cdn.discordapp.com/attachments/1170084611422949396/1445597742846968002/2526_hronek_pump.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547791392342056/hronek_goal_2025.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547792482734220/hronek_skate_goal_2025.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444546795857252393/2526_hronek_nod.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444546617473634374/2526_hronek_fan.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547792482734220/hronek_skate_goal_2025.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547791392342056/hronek_goal_2025.gif"
		));

		// Thatcher Demko
		playerGoalmessages.put(8477967, Arrays.asList(
			"https://cdn.discordapp.com/attachments/240245066017406976/1171178501886447657/demkohattilt.gif"
		));
		
		// Jake DeBrusk
		playerGoalmessages.put(8478498, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791632560422912/debrusk_cele.gif", // Cele
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791509000421407/debrusk_goal_2024.gif", // Canucks 2024
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791525857460325/debrusk_goal_skate_2024.gif", // Canucks 2024 - Skate
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547824296525855/debrusk_skate_goal_2025.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1549138623884955820/2025_debrusk.gif"
		));
		
		// Chytil
		playerGoalmessages.put(8480078, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1445597743828697100/2526_chytil_pump.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547823633698916/chytil_goal_2025.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547744852086904/2526_chytil_applause.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444546795341611059/2526_chytil_uncool.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444546615577673850/2526_chytil_fan.gif"
		));
		
		// Lekkerimäki
		playerGoalmessages.put(8483476, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1445597744575021288/2526_lek_pump.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547745409925190/2526_lek_applause.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444546796461494426/2526_lek_yes.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444546619189235896/2526_lek_num1.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1556471394202820638/lekki_goal_26.gif"
		));
		
		// Raty
		playerGoalmessages.put(8482691, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791507331354686/raty_goal_2024.gif", // Canucks 2024
			"https://cdn.discordapp.com/attachments/1170084611422949396/1549134241743507691/karpat-raty.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547347370475561/raty_goal_2025.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1549134821794779358/2025_raty_sign.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1556471260576219337/raty_goal_skate_26.gif"
		));

		// Linus Karlsson
		playerGoalmessages.put(8481024, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547787831382128/karlsson_goal_2025.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547788502339604/karlsson_skate_goal_2025.gif"
		));

		// Arshdeep Bains
		playerGoalmessages.put(8483395, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1327791627086987357/bains_goal_skate_2024.gif", // Canucks 2024 - Skate
			"https://cdn.discordapp.com/attachments/1170084611422949396/1556471396039921724/bains_goal_26.gif"
		));
		
		// Tom Willander
		playerGoalmessages.put(8484240, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1549135442224357558/2025_willander.gif"
		));
		
		// D Petey
		playerGoalmessages.put(8483678, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444546615984656554/2526_detey_fan.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444546795597467801/2526_detey_nod.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547745116454987/2526_detey_applause.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1445597744164114443/2526_detey_pump.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1556471259129315410/detey_goal_skate_26.gif"
		));
		
		// Victor Mancini
		playerGoalmessages.put(8483768, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1445597745296572476/mancini_pump.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547788858986607/mancini_applause.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547347672731692/mancini_flow.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444546796838719539/2526_mancini_yes.gif"
		));
		
		// Jamie Oleksiak
		playerGoalmessages.put(8476467, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1556471396429865000/oleksiak_goal_26.gif"
		));
		
		// DOC
		playerGoalmessages.put(8482055, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547790851280916/goal.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547743845580870/2526_doc_applause.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444546616647352380/2526_doc_num1.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547824925540412/doc_goal_2025.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1556471261025017926/doc_goal_skate_26.gif"
		));
		
		// Max Sasson
		playerGoalmessages.put(8484136, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547789668483187/glorple_goal_2025.gif",
			"https://cdn.discordapp.com/attachments/1170084611422949396/1444547790222000209/glorple_q.gif"
		));

		// Marco Rossi
		playerGoalmessages.put(8482079, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1556471393560956938/rossi_goal_26.gif"
		));
		
		// Liam Ohgren
		playerGoalmessages.put(8483499, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1556471259741556758/ohgren_goal_skate_26.gif"
		));
		
		// Paul Cotter
		playerGoalmessages.put(8481032, Arrays.asList(
			"https://cdn.discordapp.com/attachments/1170084611422949396/1556471395389542461/cotter_goal_26.gif"
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
		team("https://cdn.discordapp.com/attachments/1170084611422949396/1549124984713126069/2026_goal.gif"); // 2026 Generic 
		team("https://cdn.discordapp.com/attachments/1170084611422949396/1549128950960230561/2025_fan.gif"); // 2025 Fan
		team("https://cdn.discordapp.com/attachments/1170084611422949396/1445597743379775608/2526_lank_pump.gif"); // Lankinen
		team("https://cdn.discordapp.com/attachments/1170084611422949396/1549138623495147600/2024_goal.gif");
		
	}
}
