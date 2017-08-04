package com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.admin;

import java.util.PriorityQueue;
import java.util.Queue;

import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.CommandHelper;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource.BTBGuild;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.GuildManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;

public class SendAnnouncement extends Command {

	public SendAnnouncement() {
		this.name = "announce";
		this.help = "Send an announcement to all servers running the bot.";
		this.arguments = "message";
		this.ownerCommand = true;
		this.guildOnly = false;
	}

	@Override
	protected void execute(CommandEvent event) {
		if (CommandHelper.isBotAdmin(event.getMember())) {
			Queue<BTBGuild> guilds = new PriorityQueue<>(GuildManager.getAllGuilds());

			for (BTBGuild guild = guilds.poll(); guild != null; guild = guilds.poll()) {
				guild.sendAnnouncement(event.getArgs());
			}
		} else {
			JDAManager.sendMessage(event, "You do not have permission to do that.");
		}
	}

}
