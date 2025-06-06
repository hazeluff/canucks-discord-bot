package com.hazeluff.ahl.game;

import java.util.List;
import java.util.stream.Collectors;

import org.bson.BsonDocument;
import org.bson.BsonValue;

import com.hazeluff.ahl.game.event.Player;

public class ShootoutDetails {
	private final BsonDocument jsonDetails;

	private ShootoutDetails(BsonDocument jsonDetails) {
		this.jsonDetails = jsonDetails;
	}

	public static ShootoutDetails parse(BsonDocument jsonDetails) {
		return new ShootoutDetails(jsonDetails);
	}

	public List<ShootoutShot> getHomeShots() {
		return jsonDetails.getArray("homeTeamShots").stream()
				.map(BsonValue::asDocument)
				.map(ShootoutShot::new)
				.collect(Collectors.toList());
	}

	public List<ShootoutShot> getAwayShots() {
		return jsonDetails.getArray("visitingTeamShots").stream()
				.map(BsonValue::asDocument)
				.map(ShootoutShot::new)
				.collect(Collectors.toList());
	}
	
	public class ShootoutShot {
		private final BsonDocument jsonShot;

		private ShootoutShot(BsonDocument jsonShot) {
			this.jsonShot = jsonShot;
		}
		
		public Player getGoalie() {
			return Player.parse(jsonShot.getDocument("goalie"));
		}

		public Player getShooter() {
			return Player.parse(jsonShot.getDocument("shooter"));
		}

		public boolean isGoal() {
			return jsonShot.getBoolean("isGoal").getValue();
		}

		@Override
		public String toString() {
			return String.format("ShootoutShot [getGoalie()=%s, getShooter()=%s, isGoal()=%s]", getGoalie(),
					getShooter(), isGoal());
		}
	}

	@Override
	public String toString() {
		return String.format("ShootoutDetails [getHomeShots()=%s, getAwayShots()=%s]", getHomeShots(), getAwayShots());
	}
	
}
