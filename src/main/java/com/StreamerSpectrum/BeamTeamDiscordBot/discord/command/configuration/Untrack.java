package com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.configuration;

import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamChannel;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeam;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.CommandHelper;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.MixerManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.ConstellationManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.DbManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.GuildManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;
import com.jagrosh.jdautilities.commandclient.CommandEvent;

import net.dv8tion.jda.core.Permission;

public class Untrack extends ConfigCommand {

	private static final String	ARG_TEAM	= "team";
	private static final String	ARG_CHANNEL	= "channel";

	public Untrack() {
		super(String.format(
				"%suntrack %s **team_token_1 team_token_2...** - Removes the given teams from the server's track list.\n"
						+ "%suntrack %s **channel_name_1 channel_name_2...** - Removes the given channels from the server's track list.\n",
				JDAManager.COMMAND_PREFIX, ARG_TEAM, 
				JDAManager.COMMAND_PREFIX, ARG_CHANNEL));

		this.name = "untrack";
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
						removeTeams(event, args);
					break;
					case ARG_CHANNEL:
						removeChannels(event, args);
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

	private void removeTeams(CommandEvent event, String[] args) {
		for (int i = 1; i < args.length; ++i) {
			String teamArg = args[i];

			BeamTeam team = MixerManager.getTeam(teamArg);

			if (null != team) {
				if (GuildManager.getGuild(event.getGuild()).removeTeam(team)) {
					JDAManager.sendMessage(event,
							String.format("%s has been removed from the team tracker.", team.name));
				} else {
					JDAManager.sendMessage(event,
							String.format("%s was not found in the list of tracked teams.", team.name));
				}

				if (DbManager.readGuildsForTrackedTeam(team.id).isEmpty()) {
					ConstellationManager.unsubscribeFromTeam(team);
				}
			}
		}
	}

	private void removeChannels(CommandEvent event, String[] args) {
		for (int i = 1; i < args.length; ++i) {
			String channelArg = args[i];

			BTBBeamChannel channel = MixerManager.getChannel(channelArg);

			if (null != channel) {
				if (GuildManager.getGuild(event.getGuild()).removeChannel(channel)) {
					JDAManager.sendMessage(event,
							String.format("%s's channel has been removed from the channel tracker.", channel.token));
				} else {
					JDAManager.sendMessage(event, String
							.format("%s's channel was not found in the list of tracked channels.", channel.token));
				}
			}
		}
	}

}
