package com.StreamerSpectrum.BeamTeamDiscordBot.discord.command;

import com.StreamerSpectrum.BeamTeamDiscordBot.Constants;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;
import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;

public class Invite extends Command {

	public Invite() {
		this.name = "invite";
		this.help = Constants.INVITE_LINK;
		this.guildOnly = false;
	}
	
	@Override
	protected void execute(CommandEvent event) {
		JDAManager.sendDM(event, Constants.INVITE_LINK);
	}

}
