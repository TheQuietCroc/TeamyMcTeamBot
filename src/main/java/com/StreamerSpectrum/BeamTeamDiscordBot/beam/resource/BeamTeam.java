package com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class BeamTeam implements Serializable, Comparable<BeamTeam> {
	// DB Column Names
	public static final String		ID			= "ID";
	public static final String		OWNER_ID	= "OwnerID";
	public static final String		TOKEN		= "Token";
	public static final String		NAME		= "Name";

	public static final String[]	COLUMNS		= { ID, OWNER_ID, TOKEN, NAME };

	public int						id;
	public int						ownerId;
	public String					token;
	public String					name;
	public String					description;
	public int						totalViewersCurrent;
	public Date						createdAt;
	public Social					social;

	@Override
	public int hashCode() {
		return Integer.hashCode(id);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BeamTeam && ((BeamTeam) obj).id == this.id;
	}

	public Map<String, Object> getDbValues() {
		Map<String, Object> values = new HashMap<>();

		values.put(ID, id);
		values.put(OWNER_ID, ownerId);
		values.put(TOKEN, token);
		values.put(NAME, name);

		return values;
	}

	@Override
	public int compareTo(BeamTeam other) {
		return Integer.compare(this.id, other.id);
	}
}
