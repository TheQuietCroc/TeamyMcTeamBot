package com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class BTBBeamUser implements Serializable, Comparable<BTBBeamUser> {
	// DB Column Names
	public static final String	ID				= "ID";
	public static final String	USERNAME		= "Username";
	public static final String	BIO				= "Bio";
	public static final String	PRIMARY_TEAM	= "PrimaryTeam";
	public static final String	CHANNEL_ID		= "ChannelID";
	public static final String	SOCIAL_DISCORD	= "SocialDiscord";

	public int					level;
	public Social				social;
	public int					id;
	public String				username;
	public int					experience;
	public int					sparks;
	public String				bio;
	public Integer				primaryTeam;
	public Date					createdAt;
	public BTBBeamChannel		channel;
	public String				avatarUrl;

	public Map<String, Object> getDbValues() {
		Map<String, Object> values = new HashMap<>();

		values.put(ID, id);
		values.put(USERNAME, username);
		values.put(BIO, bio);
		values.put(PRIMARY_TEAM, primaryTeam);
		values.put(CHANNEL_ID, channel.id);
		values.put(SOCIAL_DISCORD, social.discord);

		return values;
	}

	@Override
	public int hashCode() {
		return new Integer(id).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof BeamTeamUser && ((BeamTeamUser) obj).id == this.id)
				|| (obj instanceof BTBBeamUser && ((BTBBeamUser) obj).id == this.id);
	}

	@Override
	public int compareTo(BTBBeamUser other) {
		return Integer.compare(this.id, other.id);
	}
}
