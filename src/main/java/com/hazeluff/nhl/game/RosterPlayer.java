package com.hazeluff.nhl.game;

import org.bson.BsonDocument;

import com.hazeluff.discord.nhl.NHLTeams.Team;

public class RosterPlayer {
	private final int playerId;
	private final String firstName;
	private final String lastName;
	private final String positionCode;
	private final int jerseyNumber;
	private final Team team;


	RosterPlayer(int playerId, String firstName, String lastName, String positionCode, int jerseyNumber, Team team) {
		this.playerId = playerId;
		this.firstName = firstName;
		this.lastName = lastName;
		this.positionCode = positionCode;
		this.jerseyNumber = jerseyNumber;
		this.team = team;
	}

	public static RosterPlayer parse(BsonDocument jsonPlayer) {
		return new RosterPlayer(
				jsonPlayer.getInt32("playerId").getValue(), 
				jsonPlayer.getString("fullName").getValue(), 
				jsonPlayer.getString("lastName").getValue(),
				jsonPlayer.getString("positionCode").getValue(),
				jsonPlayer.getInt32("sweaterNumber").getValue(),
				Team.parse(jsonPlayer.getInt32("teamId").getValue())
		);
	}

	public int getPlayerId() {
		return playerId;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getFullName() {
		return firstName + " " + lastName;
	}

	public String getPositionCode() {
		return positionCode;
	}

	public int getJerseyNumber() {
		return jerseyNumber;
	}

	public Team getTeam() {
		return team;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((firstName == null) ? 0 : firstName.hashCode());
		result = prime * result + jerseyNumber;
		result = prime * result + ((lastName == null) ? 0 : lastName.hashCode());
		result = prime * result + playerId;
		result = prime * result + ((positionCode == null) ? 0 : positionCode.hashCode());
		result = prime * result + ((team == null) ? 0 : team.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RosterPlayer other = (RosterPlayer) obj;
		if (firstName == null) {
			if (other.firstName != null)
				return false;
		} else if (!firstName.equals(other.firstName))
			return false;
		if (jerseyNumber != other.jerseyNumber)
			return false;
		if (lastName == null) {
			if (other.lastName != null)
				return false;
		} else if (!lastName.equals(other.lastName))
			return false;
		if (playerId != other.playerId)
			return false;
		if (positionCode == null) {
			if (other.positionCode != null)
				return false;
		} else if (!positionCode.equals(other.positionCode))
			return false;
		if (team != other.team)
			return false;
		return true;
	}

}
