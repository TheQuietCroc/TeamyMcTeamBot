package com.StreamerSpectrum.BeamTeamDiscordBot.beam.services;

import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.Costream;
import com.google.common.util.concurrent.ListenableFuture;
import com.mixer.api.MixerAPI;
import com.mixer.api.services.AbstractHTTPService;

public class CostreamsService extends AbstractHTTPService {

	public CostreamsService(MixerAPI mixer) {
		super(mixer, "costreams");
	}

	public ListenableFuture<Costream> getCostream(String id) {
		return this.get(id, Costream.class);
	}
}
