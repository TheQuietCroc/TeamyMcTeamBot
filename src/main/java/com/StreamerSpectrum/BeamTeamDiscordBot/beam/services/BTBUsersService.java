package com.StreamerSpectrum.BeamTeamDiscordBot.beam.services;

import com.StreamerSpectrum.BeamTeamDiscordBot.BTBMain;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.ratelimit.RateLimit;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamUser;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.response.BTBUserFollowsResponse;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.response.BTBUserSearchResponse;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.response.BeamTeamSearchResponse;
import com.google.common.util.concurrent.ListenableFuture;
import com.mixer.api.MixerAPI;
import com.mixer.api.http.MixerHttpClient;
import com.mixer.api.services.AbstractHTTPService;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BTBUsersService extends AbstractHTTPService {
	private final static Logger		logger		= Logger.getLogger(BTBMain.class.getName());

	private static final RateLimit	rateLimit	= new RateLimit("user-read", 500, 60);

	public BTBUsersService(MixerAPI mixer) {
		super(mixer, "users");
	}

	public ListenableFuture<BTBBeamUser> findOne(int id) {
		return this.get(String.valueOf(id), BTBBeamUser.class);
	}

	public ListenableFuture<BTBUserFollowsResponse> following(int id, int page, int limit) {
		try {
			rateLimit.isNotLimited();
		} catch (InterruptedException e) {
			logger.log(Level.WARNING, String.format("An error occurred during GET users/%d/follows", id), e);
		}
		return this.get(String.format("%d/follows", id), BTBUserFollowsResponse.class, MixerHttpClient
				.getArgumentsBuilder().put("page", Math.max(0, page)).put("limit", Math.min(limit, 50)).build());
	}

	public ListenableFuture<BeamTeamSearchResponse> teams(int id) {
		return this.get(String.format("%d/teams", id), BeamTeamSearchResponse.class);
	}

	public ListenableFuture<BTBUserSearchResponse> search(String query) {
		if (query != null && query.length() < 3) {
			throw new IllegalArgumentException(
					"unable to preform search with query less than 3 characters (was " + query.length() + ")");
		} else {
			Map<String, Object> args = MixerHttpClient.getArgumentsBuilder().put("query", query).build();

			return this.get("search", BTBUserSearchResponse.class, args);
		}
	}
}
