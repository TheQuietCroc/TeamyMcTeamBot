package com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.admin;

import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.ConstellationManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;

public class Restart extends Command {

	Thread t = null;

	public Restart() {
		this.name = "restart";
		this.help = "Restart the Mixer constellation service.";
		this.ownerCommand = true;
		this.guildOnly = false;
	}

	@Override
	protected void execute(CommandEvent event) {
		JDAManager.sendMessage(event, "Constellation restart begin...");
		ConstellationManager.start();
		JDAManager.sendMessage(event, "Constellation restart complete!");
	}

}
