package com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource;

import java.io.Serializable;
import java.util.Date;

@SuppressWarnings("serial")
public class BeamTeamUser implements Serializable, Comparable<BeamTeamUser> {
	public int				level;
	public Social			social;
	public int				id;
	public String			username;
	public int				experience;
	public int				sparks;
	public String			bio;
	public Integer			primaryTeam;
	public Date				createdAt;
	public BTBBeamChannel	channel;
	public String			avatarUrl;

	public TeamMembership	teamMembership;

	@Override
	public int hashCode() {
		return Integer.hashCode(id);
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof BeamTeamUser && ((BeamTeamUser) obj).id == this.id)
				|| (obj instanceof BTBBeamUser && ((BTBBeamUser) obj).id == this.id);
	}

	@Override
	public int compareTo(BeamTeamUser other) {
		return Integer.compare(this.id, other.id);
	}
}
