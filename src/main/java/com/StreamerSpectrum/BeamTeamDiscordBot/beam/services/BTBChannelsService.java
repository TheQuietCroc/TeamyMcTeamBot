package com.StreamerSpectrum.BeamTeamDiscordBot.beam.services;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.StreamerSpectrum.BeamTeamDiscordBot.BTBMain;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.ratelimit.RateLimit;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamChannel;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.response.BTBUserSearchResponse;
import com.google.common.util.concurrent.ListenableFuture;
import com.mixer.api.MixerAPI;
import com.mixer.api.http.MixerHttpClient;
import com.mixer.api.services.AbstractHTTPService;

public class BTBChannelsService extends AbstractHTTPService {
	private final static Logger		logger		= Logger.getLogger(BTBMain.class.getName());

	private static final RateLimit	rateLimit	= new RateLimit("channel-read", 1000, 300);

	public BTBChannelsService(MixerAPI mixer) {
		super(mixer, "channels");
	}

	public ListenableFuture<BTBBeamChannel> findOneByToken(String token) {
		try {
			rateLimit.isNotLimited();
		} catch (InterruptedException e) {
			logger.log(Level.WARNING, String.format("An error occurred during GET channels/%s", token), e);
		}

		return this.get(token, BTBBeamChannel.class);
	}

	public ListenableFuture<BTBBeamChannel> findOne(int id) {
		try {
			rateLimit.isNotLimited();
		} catch (InterruptedException e) {
			logger.log(Level.WARNING, String.format("An error occurred during GET channels/%d", id), e);
		}

		return this.get(String.valueOf(id), BTBBeamChannel.class);
	}

	public ListenableFuture<BTBUserSearchResponse> followers(BTBBeamChannel channel, int page, int limit) {
		try {
			rateLimit.isNotLimited();
		} catch (InterruptedException e) {
			logger.log(Level.WARNING, String.format("An error occurred during GET channels/%d/follow", channel.id),
					e);
		}

		return this.get(String.format("%d/follow", channel.id), BTBUserSearchResponse.class,
				MixerHttpClient.getArgumentsBuilder().put("page", Math.max(0, page))
						.put("limit", Math.min(Math.max(limit, 1), 25)).build());
	}

}
