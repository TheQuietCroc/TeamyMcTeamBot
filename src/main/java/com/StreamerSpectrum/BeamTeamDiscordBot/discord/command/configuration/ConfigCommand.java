package com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.configuration;

import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;
import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;

public abstract class ConfigCommand extends Command {

	protected static final String	ARG_HELP	= "help";
	protected final String			HELP_MSG;

	protected ConfigCommand(String helpMsg) {
		this.HELP_MSG = helpMsg;
	}
	
	protected void sendHelpMessage(CommandEvent event) {
		JDAManager.sendDM(event, HELP_MSG);
	}

	protected void sendInvalidArgumentMessage(CommandEvent event, String... args) {
		sendHelpMessage(event);

		StringBuilder sb = new StringBuilder("Invalid argument ");

		for (String arg : args) {
			sb.append(arg).append(", ");
		}

		JDAManager.sendMessage(event, sb.toString().substring(0, sb.toString().lastIndexOf(',')) + ".");
	}

}
