package com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.configuration;

import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamChannel;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeam;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.CommandHelper;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource.BTBGuild;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.MixerManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.GuildManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;
import com.jagrosh.jdautilities.commandclient.CommandEvent;

import net.dv8tion.jda.core.Permission;

public class Track extends ConfigCommand {

	private static final String	ARG_TEAM	= "team";
	private static final String	ARG_CHANNEL	= "channel";

	public Track() {
		super(String.format(
				"%strack %s **team_token_1 team_token_2...** - Adds the given teams to the server's track list. Tracked team members will be announced when when they go live; new members will be announced when they join; and team deletion, owner changes, and members joining, leaving, or going live will be logged.\n"
						+ "%strack %s **channel_name_1 channel_name_2...** - Adds the given channels to the server's track list. Tracked channels will be announced when they go live.\n",
				JDAManager.COMMAND_PREFIX, ARG_TEAM, 
				JDAManager.COMMAND_PREFIX, ARG_CHANNEL));

		this.name = "track";
		this.help = String.format("use ***%s%s help*** for more info", JDAManager.COMMAND_PREFIX, this.name);
	}

	@Override
	protected void execute(CommandEvent event) {
		String[] args = event.getArgs().toLowerCase().split(" ");

		try {
			if (CommandHelper.canUseCommand(event, new Permission[] { Permission.MANAGE_CHANNEL }, null)) {
				switch (args[0]) {
					case ARG_HELP:
						sendHelpMessage(event);
					break;
					case ARG_TEAM:
						addTeams(event, args);
					break;
					case ARG_CHANNEL:
						addChannels(event, args);
					break;
					default:
						sendInvalidArgumentMessage(event, args[0]);
					break;
				}
			}
		} catch (IndexOutOfBoundsException e) {
			JDAManager.sendMessage(event, "Unable to complete request, invalid arguments were passed to this command.");
		}
	}

	private void addTeams(CommandEvent event, String[] args) {
		BTBGuild guild = GuildManager.getGuild(event.getGuild());

		for (int i = 1; i < args.length; ++i) {
			String teamArg = args[i];
			BeamTeam team = MixerManager.getTeam(teamArg);

			if (null != team) {
				if (guild.addTeam(team)) {
					JDAManager.sendMessage(event, String.format("%s has been added to the tracker.", team.name));
				} else {
					JDAManager.sendMessage(event,
							String.format("%s is already in the list of tracked teams.", team.name));
				}
			} else {
				JDAManager.sendMessage(event, String.format("Unable to find team for token '%s'.", teamArg));
			}
		}
	}

	private void addChannels(CommandEvent event, String[] args) {
		BTBGuild guild = GuildManager.getGuild(event.getGuild());

		for (int i = 1; i < args.length; ++i) {
			String channelArg = args[i];
			BTBBeamChannel channel = MixerManager.getChannel(channelArg);

			if (null != channel) {
				if (guild.addChannel(channel)) {
					JDAManager.sendMessage(event, String.format("%s's channel has been added to the tracker.",
							MixerManager.getUser(channel.userId).username));
				} else {
					JDAManager.sendMessage(event,
							String.format("%s's channel is already in the list of tracked channels.",
									MixerManager.getUser(channel.userId).username));
				}
			}
		}
	}

}
