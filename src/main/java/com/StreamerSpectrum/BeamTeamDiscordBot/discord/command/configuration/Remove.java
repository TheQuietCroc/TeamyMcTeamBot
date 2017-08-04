package com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.configuration;

import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeam;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.CommandHelper;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource.BTBGuild;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.MixerManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.DbManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.GuildManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;
import com.jagrosh.jdautilities.commandclient.CommandEvent;

import net.dv8tion.jda.core.Permission;

public class Remove extends ConfigCommand {

	private static final String	ARG_CHANNEL			= "channel";
	private static final String	ARG_ROLE			= "role";

	private static final String	ARG_ANNOUNCEMENTS	= "announcements";
	private static final String	ARG_GO_LIVE			= "go_live";
	private static final String	ARG_LOG				= "log";
	private static final String	ARG_NEW_MEMBER		= "new_member";
	private static final String	ARG_LFM				= "lfm";

	private static final String	ARG_TEAM			= "team";

	public Remove() {
		super(String.format(
				"%sremove %s %s - Removes the announcement channel from this server's configuration.\n"
						+ "%sremove %s %s - Removes the go-live channel from this server's configuration.\n"
						+ "%sremove %s %s - Removes the log channel from this server's configuration.\n"
						+ "%sremove %s %s - Removes the new member announcement channel from this server's configuration.\n"
						+ "%sremove %s %s - Removes the lfm channel from this server's configuration.\n"
						+ "%sremove %s %s **team_token** - Removes the team role from this server's configuration.\n",
				JDAManager.COMMAND_PREFIX, ARG_CHANNEL, ARG_ANNOUNCEMENTS, 
				JDAManager.COMMAND_PREFIX, ARG_CHANNEL, ARG_GO_LIVE, 
				JDAManager.COMMAND_PREFIX, ARG_CHANNEL, ARG_LOG, 
				JDAManager.COMMAND_PREFIX, ARG_CHANNEL,	ARG_NEW_MEMBER,
				JDAManager.COMMAND_PREFIX, ARG_CHANNEL,	ARG_LFM, 
				JDAManager.COMMAND_PREFIX, ARG_ROLE, ARG_TEAM));

		this.name = "remove";
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
				case ARG_CHANNEL:
					if (CommandHelper.canUseCommand(event, new Permission[] { Permission.MANAGE_CHANNEL }, null)) {
						switch (args[1]) {
							case ARG_HELP:
								sendHelpMessage(event);
							break;
							case ARG_ANNOUNCEMENTS:
								removeAnnouncementChannel(event);
							break;
							case ARG_GO_LIVE:
								removeGoLiveChannel(event);
							break;
							case ARG_LOG:
								removeLogChannel(event);
							break;
							case ARG_NEW_MEMBER:
								removeNewMemberChannel(event);
							break;
							default:
								sendInvalidArgumentMessage(event, args[1]);
							break;
						}
					}
				break;
				case ARG_ROLE:
					if (CommandHelper.canUseCommand(event, new Permission[] { Permission.MANAGE_ROLES }, null)) {
						switch (args[1]) {
							case ARG_TEAM:
								removeTeamRole(event, args[2]);
							break;
							default:
								sendInvalidArgumentMessage(event, args[1]);
							break;
						}
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

	private void removeTeamRole(CommandEvent event, String teamToken) {
		BeamTeam team = MixerManager.getTeam(teamToken);

		if (null != team) {
			if (DbManager.deleteTeamRole(Long.parseLong(event.getGuild().getId()), team.id)) {
				JDAManager.sendMessage(event, String.format("Successfully removed %s's role!", team.name));
			} else {
				JDAManager.sendMessage(event,
						String.format(
								"I was unable to remove %s's role from your configuration. Please make sure that team has a role set.",
								team.name));
			}
		}
	}

	private void removeNewMemberChannel(CommandEvent event) {
		BTBGuild guild = GuildManager.getGuild(event.getGuild());

		if (guild.getNewMemberChannelID() != null) {
			guild.setNewMemberChannelID(null);

			JDAManager.sendMessage(event,
					"New member announcement channel has been removed from this server's configurations.");
		} else {
			JDAManager.sendMessage(event, "There is no new member announcement channel set for this server.");
		}
	}

	private void removeLogChannel(CommandEvent event) {
		BTBGuild guild = GuildManager.getGuild(event.getGuild());

		if (guild.getLogChannelID() != null) {
			guild.setLogChannelID(null);

			JDAManager.sendMessage(event, "Log channel has been removed from this server's configurations.");
		} else {
			JDAManager.sendMessage(event, "There is no log channel set for this server.");
		}
	}

	private void removeGoLiveChannel(CommandEvent event) {
		BTBGuild guild = GuildManager.getGuild(event.getGuild());

		if (guild.getGoLiveChannelID() != null) {
			guild.setGoLiveChannelID(null);
			JDAManager.sendMessage(event, "Go-live channel has been removed from this server's configurations.");
		} else {
			JDAManager.sendMessage(event, "There is no go-live channel set for this server.");
		}
	}

	private void removeAnnouncementChannel(CommandEvent event) {
		BTBGuild guild = GuildManager.getGuild(event.getGuild());

		if (guild.getAnnouncementChannelID() != null) {
			guild.setAnnouncementChannelID(null);

			JDAManager.sendMessage(event,
					"Beam and bot announcement channel has been removed from this server's configurations.");
		} else {
			JDAManager.sendMessage(event, "There is no Beam and bot announcement channel set for this server.");
		}
	}

}
