package com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.configuration;

import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.commons.lang3.StringUtils;

import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamChannel;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeam;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.CommandHelper;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource.BTBGuild;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource.BTBRole;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.MixerManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.DbManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.GuildManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;
import com.jagrosh.jdautilities.commandclient.CommandEvent;

import net.dv8tion.jda.core.Permission;

public class Show extends ConfigCommand {

	private static final String	ARG_CONFIGS		= "configs";
	private static final String	ARG_TEAMS		= "teams";
	private static final String	ARG_CHANNELS	= "channels";
	private static final String	ARG_TEAMROLES	= "team_roles";

	public Show() {
		super(String.format(
				"%sshow %s - Displays the configurations set for this server.\n"
						+ "%sshow %s - Displays the teams this server is tracking.\n"
						+ "%sshow %s - Displays the channels this server is tracking.\n"
						+ "%sshow %s - Displays the team roles in use on this server.\n",
				JDAManager.COMMAND_PREFIX, ARG_CONFIGS, 
				JDAManager.COMMAND_PREFIX, ARG_TEAMS, 
				JDAManager.COMMAND_PREFIX, ARG_CHANNELS, 
				JDAManager.COMMAND_PREFIX, ARG_TEAMROLES));

		this.name = "show";
		this.help = String.format("use ***%s%s help*** for more info", JDAManager.COMMAND_PREFIX, this.name);
	}

	@Override
	protected void execute(CommandEvent event) {
		String[] args = event.getArgs().toLowerCase().split(" ");

		try {
			switch (args[0]) {
				case ARG_HELP:
					sendHelpMessage(event);
				break;
				case ARG_CONFIGS:
					if (CommandHelper.canUseCommand(event, new Permission[] { Permission.MANAGE_CHANNEL }, null)
							|| CommandHelper.canUseCommand(event, new Permission[] { Permission.MANAGE_ROLES }, null)) {
						showConfigs(event);
					}
				break;
				case ARG_TEAMS:
					if (CommandHelper.canUseCommand(event, new Permission[] { Permission.MANAGE_CHANNEL }, null)) {
						showTeams(event);
					}
				break;
				case ARG_CHANNELS:
					if (CommandHelper.canUseCommand(event, new Permission[] { Permission.MANAGE_CHANNEL }, null)) {
						showChannels(event);
					}
				break;
				case ARG_TEAMROLES:
					if (CommandHelper.canUseCommand(event, new Permission[] { Permission.MANAGE_ROLES }, null)) {
						showTeamRoles(event);
					}
				break;
				default:
					sendInvalidArgumentMessage(event, args[0]);
				break;
			}
		} catch (IndexOutOfBoundsException e) {
			JDAManager.sendMessage(event, "Unable to complete request, invalid arguments were passed to this command.");
		}
	}

	private void showTeamRoles(CommandEvent event) {
		Queue<BTBRole> roles = new PriorityQueue<>(
				DbManager.readTeamRolesForGuild(Long.parseLong(event.getGuild().getId())));

		StringBuilder teamRolesSB = new StringBuilder();

		for (BTBRole role = roles.poll(); role != null; role = roles.poll()) {
			teamRolesSB.append(String.format("%s - %s\n", MixerManager.getTeam(role.getTeamID()).name,
					event.getGuild().getRoleById(role.getRoleID()).getName()));
		}

		if (StringUtils.isBlank(teamRolesSB.toString())) {
			teamRolesSB.append("NONE");
		}

		CommandHelper.sendPagination(event, teamRolesSB.toString().split("\n"), 1,
				String.format("Team roles for %s: ", event.getGuild().getName()));
	}

	private void showConfigs(CommandEvent event) {
		BTBGuild guild = GuildManager.getGuild(event.getGuild());

		StringBuilder configSB = new StringBuilder(
				String.format("Beam Team Bot configurations for %s:", guild.getName()));

		configSB.append("\nGo-live Channel: ");
		if (StringUtils.isBlank(guild.getGoLiveChannelID())) {
			configSB.append("Not configured");
		} else {
			configSB.append(event.getGuild().getTextChannelById(guild.getGoLiveChannelID()).getAsMention());
		}

		configSB.append("\nRemove Offline Channel Messages: ").append(guild.isRemoveOfflineChannelAnnouncements());

		configSB.append("\nGo-live Role: ");
		if (StringUtils.isBlank(guild.getGoLiveRoleID())) {
			configSB.append("Not configured");
		} else {
			configSB.append(event.getGuild().getRoleById(guild.getGoLiveRoleID()).getAsMention());
		}

		configSB.append("\nNew Member Announcement Channel: ");
		if (StringUtils.isBlank(guild.getNewMemberChannelID())) {
			configSB.append("Not configured");
		} else {
			configSB.append(event.getGuild().getTextChannelById(guild.getNewMemberChannelID()).getAsMention());
		}

		configSB.append("\nLog Channel: ");
		if (StringUtils.isBlank(guild.getLogChannelID())) {
			configSB.append("Not configured");
		} else {
			configSB.append(event.getGuild().getTextChannelById(guild.getLogChannelID()).getAsMention());
		}

		configSB.append("\nAnnouncement Channel: ");
		if (StringUtils.isBlank(guild.getAnnouncementChannelID())) {
			configSB.append("Not configured");
		} else {
			configSB.append(event.getGuild().getTextChannelById(guild.getAnnouncementChannelID()).getAsMention());
		}

		JDAManager.sendMessage(event, configSB.toString());
	}

	private void showTeams(CommandEvent event) {
		Queue<BeamTeam> teams = new PriorityQueue<>(GuildManager.getGuild(event.getGuild()).getTrackedTeams());

		if (teams.size() > 0) {
			StringBuilder teamsSB = new StringBuilder();

			BeamTeam team = null;
			while ((team = teams.poll()) != null) {
				teamsSB.append(team.token).append("\n");
			}

			CommandHelper.sendPagination(event, teamsSB.toString().split("\n"), 1,
					"This server is tracking the following Teams:");
		} else {
			JDAManager.sendMessage(event, "This server is not tracking any teams.");
		}
	}

	private void showChannels(CommandEvent event) {
		Queue<BTBBeamChannel> channels = new PriorityQueue<>(
				GuildManager.getGuild(event.getGuild()).getTrackedChannels());

		if (channels.size() > 0) {
			StringBuilder channelSB = new StringBuilder();

			for (BTBBeamChannel channel = channels.poll(); channel != null; channel = channels.poll()) {
				channelSB.append(channel.token).append("\n");
			}

			CommandHelper.sendPagination(event, channelSB.toString().split("\n"), 1,
					"This server is tracking the following Channels:");
		} else {
			JDAManager.sendMessage(event, "This server is not tracking any channels.");
		}
	}

}
