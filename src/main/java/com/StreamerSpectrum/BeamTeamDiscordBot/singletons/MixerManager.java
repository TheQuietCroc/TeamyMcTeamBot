package com.StreamerSpectrum.BeamTeamDiscordBot.singletons;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.StreamerSpectrum.BeamTeamDiscordBot.BTBMain;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamChannel;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamUser;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeam;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeamUser;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.Costream;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.response.BTBUserFollowsResponse;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.response.TeamUserSearchResponse;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.services.BTBChannelsService;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.services.BTBUsersService;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.services.CostreamsService;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.services.TeamsService;
import com.auth0.jwt.internal.org.apache.commons.lang3.StringUtils;
import com.mixer.api.MixerAPI;
import com.mixer.api.resource.constellation.MixerConstellation;

public abstract class MixerManager {
	private final static Logger			logger	= Logger.getLogger(BTBMain.class.getName());

	private static MixerAPI				mixer;
	private static MixerConstellation	constellation;

	public static MixerAPI getMixer() {
		if (null == mixer) {
			mixer = new MixerAPI();

			mixer.register(new TeamsService(mixer));
			mixer.register(new BTBChannelsService(mixer));
			mixer.register(new BTBUsersService(mixer));
			mixer.register(new CostreamsService(mixer));
		}

		return mixer;
	}

	public static MixerConstellation getConstellation() {
		if (null == constellation) {
			constellation = new MixerConstellation();
		}

		return constellation;
	}

	public static BTBBeamUser getUser(int id) {
		BTBBeamUser user = null;

		try {
			user = getMixer().use(BTBUsersService.class).findOne(id).get();
		} catch (InterruptedException | ExecutionException e) {
			logger.log(Level.WARNING, String.format("Something went wrong when attempting to find user %d.", id), e);
		}

		return user;
	}

	public static BTBBeamUser getUser(String name) {
		BTBBeamUser user = null;

		try {
			user = getMixer().use(BTBUsersService.class).search(name).get().get(0);
		} catch (InterruptedException | ExecutionException e) {
			logger.log(Level.WARNING, String.format("Something went wrong when attempting to find user %s.", name), e);
		}

		return user;
	}

	public static BeamTeam getTeam(String token) {
		BeamTeam team = null;

		try {
			team = getMixer().use(TeamsService.class).findOne(token).get();
		} catch (InterruptedException | ExecutionException e) {
			logger.log(Level.WARNING, String.format("Something went wrong when attempting to find team %s.", token), e);
		}

		return team;
	}

	public static BeamTeam getTeam(int id) {
		BeamTeam team = null;

		try {
			team = getMixer().use(TeamsService.class).findOne(id).get();
		} catch (InterruptedException | ExecutionException e) {
			logger.log(Level.WARNING, String.format("Something went wrong when attempting to find team %d.", id), e);
		}

		return team;
	}

	public static List<BeamTeamUser> getTeamMembers(String team) {
		return getTeamMembers(getTeam(team));
	}

	public static List<BeamTeamUser> getTeamMembers(int id) {
		TeamUserSearchResponse teamMembers = new TeamUserSearchResponse();
		int page = 0;

		try {
			while (teamMembers.addAll(getMixer().use(TeamsService.class).teamMembersOf(id, page++, 50).get()));
		} catch (NullPointerException | InterruptedException | ExecutionException e) {
			logger.log(Level.WARNING,
					String.format("Something went wrong when attempting to find team members from %d.", id), e);
		}

		return teamMembers;
	}

	public static List<BeamTeamUser> getTeamMembers(BeamTeam team) {
		return getTeamMembers(team.id);
	}

	public static List<BTBBeamChannel> getFollowing(int id) {
		BTBUserFollowsResponse following = new BTBUserFollowsResponse();

		int page = 0;

		try {
			while (following.addAll(getMixer().use(BTBUsersService.class).following(id, page++, 50).get()));
		} catch (InterruptedException | ExecutionException e) {
			logger.log(Level.WARNING,
					String.format("Something went wrong when attempting to find following for user id %d.", id), e);
		}

		return following;
	}

	public static BTBBeamChannel getChannel(int id) {
		BTBBeamChannel channel = getChannelFromDB(id);

		if (null == channel) {
			channel = getChannelFromMixer(id);
			DbManager.createChannel(channel);
		}

		return channel;
	}

	public static BTBBeamChannel getChannelFromDB(int id) {
		return DbManager.readChannel(id);
	}

	public static BTBBeamChannel getChannelFromMixer(int id) {
		BTBBeamChannel channel = null;

		for (int retry = 0; channel == null && retry < 10; ++retry) {
			try {
				channel = getMixer().use(BTBChannelsService.class).findOne(id).get();

				if (channel == null) {
					TimeUnit.SECONDS.sleep(1);
				} else {
					break;
				}
			} catch (InterruptedException | ExecutionException e) {
				logger.log(Level.WARNING, String.format("Something went wrong when attempting to find channel %d.", id),
						e);
			}
		}

		return channel;
	}

	public static BTBBeamChannel getChannel(String name) {
		BTBBeamChannel channel = getChannelFromDB(name);

		if (null == channel) {
			channel = getChannelFromMixer(name);
			DbManager.createChannel(channel);
		}

		return channel;
	}

	public static BTBBeamChannel getChannelFromDB(String name) {
		return DbManager.readChannel(name);
	}

	public static BTBBeamChannel getChannelFromMixer(String name) {
		BTBBeamChannel channel = null;

		try {
			for (int retry = 0; channel == null && retry < 10; ++retry) {
				channel = getMixer().use(BTBChannelsService.class).findOneByToken(name).get();

				if (channel == null) {
					TimeUnit.SECONDS.sleep(1);
				}
			}
		} catch (InterruptedException | ExecutionException e) {
			logger.log(Level.WARNING, String.format("Something went wrong when attempting to find channel %s.", name),
					e);
		}

		return channel;
	}

	public static List<BeamTeam> getTeams(BTBBeamUser user) {
		return getTeams(user.id);
	}

	public static List<BeamTeam> getTeams(int id) {
		List<BeamTeam> teams = new ArrayList<>();

		try {
			teams = getMixer().use(BTBUsersService.class).teams(id).get();
		} catch (InterruptedException | ExecutionException e) {
			logger.log(Level.WARNING,
					String.format("Something went wrong when attempting to find user %d's teams.", id), e);
		}

		return teams;
	}

	public static Costream getCostream(BTBBeamChannel channel) {
		return getCostream(channel.costreamId);
	}

	public static Costream getCostream(String costreamId) {
		Costream costream = null;

		if (StringUtils.isNotBlank(costreamId)) {
			try {
				costream = getMixer().use(CostreamsService.class).getCostream(costreamId).get();
			} catch (Exception ignore) {}
		}

		return costream;
	}
}
