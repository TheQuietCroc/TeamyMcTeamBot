package com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource;

import java.util.HashMap;
import java.util.Map;

public class BTBRole implements Comparable<BTBRole> {

	// DB Column Names
	public static final String	GuildID	= "GuildID";
	public static final String	TeamID	= "TeamID";
	public static final String	RoleID	= "RoleID";

	private final long			guildID;
	private int					teamID;
	private String				roleID;

	public BTBRole(long guildID, int teamID, String roleID) {
		this.guildID = guildID;
		this.teamID = teamID;
		this.roleID = roleID;
	}

	public BTBRole(Map<String, String> values) {
		this.guildID = Long.parseLong(values.get(GuildID));
		this.teamID = Integer.parseInt(values.get(TeamID));
		this.roleID = values.get(roleID);
	}

	public long getGuildID() {
		return guildID;
	}

	public int getTeamID() {
		return teamID;
	}

	public void setTeamID(int teamID) {
		this.teamID = teamID;
	}

	public String getRoleID() {
		return roleID;
	}

	public void setRoleID(String roleID) {
		this.roleID = roleID;
	}

	public Map<String, Object> getDbValues() {
		Map<String, Object> values = new HashMap<>();

		values.put(GuildID, getGuildID());
		values.put(RoleID, getRoleID());
		values.put(TeamID, getTeamID());

		return values;
	}

	@Override
	public int compareTo(BTBRole other) {
		return Long.compare(Long.parseLong(this.roleID), Long.parseLong(other.roleID));
	}
}
