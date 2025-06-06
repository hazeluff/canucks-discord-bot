package com.hazeluff.ahl.game.event;

import org.bson.BsonDocument;

public class Player {
	private final BsonDocument jsonPlayer;

	private Player(BsonDocument jsonPlayer) {
		this.jsonPlayer = jsonPlayer;
	}

	public static Player parse(BsonDocument jsonPlayer) {
		return new Player(jsonPlayer);
	}

	public int getId() {
		return jsonPlayer.getInt32("id").getValue();
	}

	public String getFirstName() {
		if (jsonPlayer.isNull("firstName")) {
			return "";
		}
		return jsonPlayer.getString("firstName").getValue();
	}

	public String getLastName() {
		if (jsonPlayer.isNull("lastName")) {
			return "";
		}
		return jsonPlayer.getString("lastName").getValue();
	}

	public String getFullName() {
		return getFirstName() + " " + getLastName();
	}

	public int getJerseyNumber() {
		return jsonPlayer.getInt32("jerseyNumber").getValue();
	}

	@Override
	public String toString() {
		return String.format("Player [getId()=%s, getFirstName()=%s, getLastName()=%s, getJerseyNumber()=%s]", 
				getId(), getFirstName(), getLastName(), getJerseyNumber());
	}
}
