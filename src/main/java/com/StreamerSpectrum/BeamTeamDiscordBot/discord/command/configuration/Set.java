package com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.configuration;

import java.util.List;

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
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;

public class Set extends ConfigCommand {

	private static final String	ARG_CHANNEL			= "channel";
	private static final String	ARG_ROLE			= "role";

	private static final String	ARG_ANNOUNCEMENTS	= "announcements";
	private static final String	ARG_GO_LIVE			= "go_live";
	private static final String	ARG_LOG				= "log";
	private static final String	ARG_NEW_MEMBER		= "new_member";
	private static final String	ARG_LFM				= "lfm";

	private static final String	ARG_TEAM			= "team";

	public Set() {
		super(String.format(
				"%sset %s %s **text_channel_name** - Sets the designated channel to display Mixer and Bot announcements in.\n"
						+ "%sset %s %s **text_channel_name** - Sets the designated channel to display go-live messages for tracked streams.\n"
						+ "%sset %s %s **text_channel_name** - Sets the designated channel to display log messages from the bot.\n"
						+ "%sset %s %s **text_channel_name** - Sets the designated channel to display new member announcements.\n"
						+ "%sset %s %s **text_channel_name** - Sets the designated channel to display LFM broadcasts.\n"
						+ "%sset %s %s **team_token** **role_name** - Assigns the designated Discord role to members of the given Mixer team.\n",
				JDAManager.COMMAND_PREFIX, ARG_CHANNEL, ARG_ANNOUNCEMENTS, 
				JDAManager.COMMAND_PREFIX, ARG_CHANNEL, ARG_GO_LIVE, 
				JDAManager.COMMAND_PREFIX, ARG_CHANNEL, ARG_LOG, 
				JDAManager.COMMAND_PREFIX, ARG_CHANNEL, ARG_NEW_MEMBER, 
				JDAManager.COMMAND_PREFIX, ARG_CHANNEL, ARG_LFM, 
				JDAManager.COMMAND_PREFIX, ARG_ROLE, ARG_TEAM));

		this.name = "set";
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
								setAnnouncementChannel(event, args[2]);
							break;
							case ARG_GO_LIVE:
								setGoLiveChannel(event, args[2]);
							break;
							case ARG_LOG:
								setLogChannel(event, args[2]);
							break;
							case ARG_NEW_MEMBER:
								setNewMemberChannel(event, args[2]);
							break;
							case ARG_LFM:
								setLFMChannel(event, args[2]);
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
								setTeamRole(event, args);
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

	private void setAnnouncementChannel(CommandEvent event, String channelID) {
		TextChannel channel = getChannel(event, channelID);

		if (null != channel) {
			GuildManager.getGuild(event.getGuild()).setAnnouncementChannelID(channel.getId());

			JDAManager.sendMessage(event,
					String.format("Beam and Bot announcement channel has been set to %s.", channel.getAsMention()));
		}
	}

	private void setGoLiveChannel(CommandEvent event, String channelID) {
		TextChannel channel = getChannel(event, channelID);

		if (null != channel) {
			GuildManager.getGuild(event.getGuild()).setGoLiveChannelID(channel.getId());

			JDAManager.sendMessage(event, String.format("Go-live channel has been set to %s.", channel.getAsMention()));
		}
	}

	private void setLogChannel(CommandEvent event, String channelID) {
		TextChannel channel = getChannel(event, channelID);

		if (null != channel) {
			GuildManager.getGuild(event.getGuild()).setLogChannelID(channel.getId());

			JDAManager.sendMessage(event, String.format("Log channel has been set to %s.", channel.getAsMention()));
		}
	}

	private void setNewMemberChannel(CommandEvent event, String arg) {
		TextChannel channel = getChannel(event, arg);

		if (null != channel) {
			GuildManager.getGuild(event.getGuild()).setNewMemberChannelID(channel.getId());

			JDAManager.sendMessage(event,
					String.format("New member channel has been set to %s.", channel.getAsMention()));
		}
	}

	private void setLFMChannel(CommandEvent event, String arg) {
		TextChannel channel = getChannel(event, arg);

		if (null != channel) {
			GuildManager.getGuild(event.getGuild()).setLFMChannelID(channel.getId());

			JDAManager.sendMessage(event, String.format("LFM channel has been set to %s.", channel.getAsMention()));
		}
	}

	private void setTeamRole(CommandEvent event, String[] args) throws IndexOutOfBoundsException {
		String teamArg = args[2];

		BeamTeam team = MixerManager.getTeam(teamArg);

		if (null != team) {
			String roleArg = "";

			for (int i = 3; i < args.length; ++i) {
				roleArg += args[i] + " ";
			}

			roleArg = roleArg.trim();

			BTBGuild guild = GuildManager.getGuild(event.getGuild());

			Role role = event.getGuild().getRolesByName(roleArg, true).isEmpty() ? null
					: event.getGuild().getRolesByName(roleArg, true).get(0);

			if (null != role) {
				BTBRole teamRole = new BTBRole(guild.getID(), team.id, role.getId());

				if (DbManager.createTeamRole(teamRole)) {
					JDAManager.distributeTeamRoleToGuildTeamMembers(teamRole);

					JDAManager.sendMessage(event, String.format(
							"The role '%s' will now be added to all new %s members.", role.getName(), team.name));
				} else {
					JDAManager.sendMessage(event, String.format("I am unable to associate the '%s' role with %s.",
							role.getName(), team.name));
				}
			} else {
				JDAManager.sendMessage(event,
						String.format("I cannot find a role named '%s' on this server.", roleArg));
			}
		}
	}

	private TextChannel getChannel(CommandEvent event, String channelName) {
		List<TextChannel> channels = event.getJDA().getTextChannelsByName(channelName.replaceAll("#", ""), true);

		if (channels.isEmpty()) {
			JDAManager.sendMessage(event,
					String.format(
							"I can't find the channel named %s. Please ensure it exists and that I have read & write priveleges for it.",
							channelName.replaceAll("#", "")));

			return null;
		} else {
			return channels.get(0);
		}
	}
}
