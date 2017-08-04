package com.StreamerSpectrum.BeamTeamDiscordBot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.security.auth.login.LoginException;

import org.apache.commons.logging.LogFactory;

import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamChannel;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamUser;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeam;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeamUser;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.MixerManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.ConstellationManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.DbManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.LFMManager;

import net.dv8tion.jda.core.exceptions.RateLimitedException;

public class BTBMain {
	private final static Logger	logger	= Logger.getLogger(BTBMain.class.getName());
	private static FileHandler	fh		= null;

	public static void main(String[] args) throws LoginException, RateLimitedException, InterruptedException {
		initLogger();

		startupSync();
		ConstellationManager.start();
		LFMManager.init();
	}

	public static void startupSync() {
		// JDAManager.getSelfUser().getJDA().getPresence().setPresence(OnlineStatus.DO_NOT_DISTURB,
		// Game.of("Synching Mixer cache"));
		logger.log(Level.INFO, "Syncing cache with Mixer");

		Queue<BeamTeam> teams = new LinkedList<>(DbManager.readAllTeams());
		Queue<BTBBeamChannel> channels = new LinkedList<>();

		for (BeamTeam team = teams.poll(); team != null; team = teams.poll()) {
			try {
				DbManager.updateTeam(MixerManager.getTeam(team.id));
			} catch (Exception e) {
				logger.log(Level.INFO, String.format("Unable to find team \"%s\" on Mixer.", team.token), e);
				continue;
			}

			for (BeamTeamUser member : MixerManager.getTeamMembers(team.id)) {
				BTBBeamUser user = new BTBBeamUser();
				user.avatarUrl = member.avatarUrl;
				user.bio = member.bio;
				user.social = member.social;

				member.channel.user = user;
				channels.add(member.channel);
			}
		}

		for (BTBBeamChannel channel : DbManager.readAllTrackedChannels()) {
			if (!channels.contains(channel)) {
				try {
					channels.add(MixerManager.getChannelFromMixer(channel.id));
				} catch (Exception e) {
					logger.log(Level.WARNING,
							String.format("An issue ocurred when trying to sync %s's channel.", channel.token), e);
				}
			}
		}

		for (BTBBeamChannel channel = channels.poll(); channel != null; channel = channels.poll()) {
			DbManager.updateChannel(channel);
		}

		logger.log(Level.INFO, "Mixer cache sync complete!");
		// JDAManager.getSelfUser().getJDA().getPresence().setPresence(OnlineStatus.ONLINE,
		// Game.of("Type !tb help"));
	}

	private static void initLogger() {
		if (Files.notExists(Paths.get("logs\\"))) {
			new File("logs\\").mkdir();
		}

		LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log",
				"org.apache.commons.logging.impl.NoOpLog");
		java.util.logging.Logger.getLogger("org.apache.http.client.protocol.ResponseProcessCookies")
				.setLevel(Level.OFF);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_SSS");

		try {
			fh = new FileHandler("logs\\BTB-Logger_" + sdf.format(new Date()) + ".log", false);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
		Logger l = Logger.getLogger("");
		fh.setFormatter(new SimpleFormatter());
		l.addHandler(fh);
		l.setLevel(Level.INFO);

		logger.log(Level.INFO, "Logger initialized");
	}

}
