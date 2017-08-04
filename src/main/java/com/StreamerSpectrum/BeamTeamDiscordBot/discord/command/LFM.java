package com.StreamerSpectrum.BeamTeamDiscordBot.discord.command;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.LFMManager;
import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;

public class LFM extends Command {

	private final String helpMsg = String.format(
			"%s %s **duration \"message in quotes\"** - **duration** is written as a number with a suffix i.e. 1m 1h 1d for 1 minute, 1 hour, and 1 day respectively. This is how long you want your message to stay live for. Your LFM message must be in quotes.\n"
					+ "%s %s **cancel** - cancels your current LFM broadcast immediately",
			JDAManager.COMMAND_PREFIX, "lfm", JDAManager.COMMAND_PREFIX, "lfm");

	public LFM() {
		this.name = "lfm";
		this.help = String.format("use ***%s %s help*** for more info", JDAManager.COMMAND_PREFIX, this.name);
	}

	@Override
	protected void execute(CommandEvent event) {
		try {
			String arg = event.getArgs();

			if ("help".equalsIgnoreCase(arg)) {
				JDAManager.sendDM(event, helpMsg);
			} else if ("cancel".equalsIgnoreCase(arg)) {
				if (!LFMManager.cancelLFMForUser(event.getAuthor().getIdLong())) {
					JDAManager.sendMessage(event, String.format("%s, you do not have a message in the LFM broadcast.",
							event.getAuthor().getAsMention()));
				}
			} else if (arg.substring(0, arg.indexOf("\"")).trim().matches("^\\d+[dhm]$")) {
				String timeArg = arg.substring(0, arg.indexOf("\"")).trim();
				String messageArg = arg.substring(arg.indexOf("\"")).replaceAll("@", "(at)");
				Date deleteTime = new Date();

				switch (timeArg.charAt(timeArg.length() - 1)) {
					case 'm': {
						int minutes = Math.max(1, Math.min(4320, Integer.parseInt(timeArg.replace("m", ""))));
						deleteTime.setTime(deleteTime.getTime() + TimeUnit.MINUTES.toMillis(minutes));
					}
					break;
					case 'h': {
						int hours = Math.max(1, Math.min(72, Integer.parseInt(timeArg.replace("h", ""))));
						deleteTime.setTime(deleteTime.getTime() + TimeUnit.HOURS.toMillis(hours));
					}
					break;
					case 'd': {
						int days = Math.max(1, Math.min(3, Integer.parseInt(timeArg.replace("d", ""))));
						deleteTime.setTime(deleteTime.getTime() + TimeUnit.DAYS.toMillis(days));
					}
					break;
					default:
					break;
				}

				if (!LFMManager.sendBroadcast(event, deleteTime.getTime(), messageArg)) {
					JDAManager.sendMessage(event,
							String.format(
									"%s, you already have an LFM broadcast message. Type !tb lfm cancel to erase it and try again.",
									event.getAuthor().getAsMention()));
				}

			} else {
				JDAManager.sendDM(event, helpMsg);
			}
		} catch (Exception ex) {
			JDAManager.sendMessage(event, "Oops, something went wrong. Please try that command again.");
			JDAManager.sendDM(event, helpMsg);
		}
	}
}
