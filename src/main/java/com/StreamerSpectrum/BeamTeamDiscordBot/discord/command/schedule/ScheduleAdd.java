package com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.schedule;

import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.CommandHelper;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import net.dv8tion.jda.core.Permission;

public class ScheduleAdd extends Command {

	final Permission[] permissions = { Permission.MANAGE_CHANNEL };

	public ScheduleAdd() {
		this.name = "scheduleadd";
		this.help = "Adds a streamer to the schedule list.";
		this.arguments = "sun/mon/tues/weds/thurs/fri/sat/once message";
	}

	@Override
	protected void execute(CommandEvent event) {
		if (CommandHelper.canUseCommand(event, permissions, null)) {
			String day = event.getArgs().substring(0, event.getArgs().indexOf(" ")).toLowerCase();
			String message = event.getArgs().substring(event.getArgs().indexOf(" ") + 1);

			switch (day) {
				case "sun": {

				}
				break;
				case "mon": {

				}
				break;
				case "tues": {

				}
				break;
				case "weds": {

				}
				break;
				case "thurs": {

				}
				break;
				case "fri": {

				}
				break;
				case "sat": {

				}
				break;
				case "once": {

				}
				break;
				default:
					JDAManager.sendMessage(event, "Invalid day argument!");
				break;
			}
		}
	}

}
