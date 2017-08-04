package com.StreamerSpectrum.BeamTeamDiscordBot.beam.services;

import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeam;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.response.TeamUserSearchResponse;
import com.google.common.util.concurrent.ListenableFuture;
import com.mixer.api.MixerAPI;
import com.mixer.api.http.MixerHttpClient;
import com.mixer.api.services.AbstractHTTPService;

public class TeamsService extends AbstractHTTPService {

	public TeamsService(MixerAPI mixer) {
		super(mixer, "teams");
	}

	public ListenableFuture<BeamTeam> findOne(int id) {
		return this.get(String.valueOf(id), BeamTeam.class);
	}

	public ListenableFuture<BeamTeam> findOne(String token) {
		return this.get(token, BeamTeam.class);
	}

	public ListenableFuture<TeamUserSearchResponse> teamMembersOf(int teamID, int page, int limit) {
		return this.get(String.format("%d/users", teamID), TeamUserSearchResponse.class,
				MixerHttpClient.getArgumentsBuilder().put("id", teamID).put("page", Math.max(0, page))
						.put("limit", Math.min(50, Math.max(1, limit))).build());
	}
}
