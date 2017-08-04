package com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.teamrole;

import java.util.PriorityQueue;
import java.util.Queue;

import com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource.BTBRole;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.DbManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import net.dv8tion.jda.core.Permission;

public class TeamRoleDistribute extends Command {

	public TeamRoleDistribute() {
		this.name = "teamroledistribute";
		this.help = "Distributes this server's team roles to their members.";
		this.userPermissions = new Permission[] { Permission.MANAGE_ROLES };
	}

	@Override
	protected void execute(CommandEvent event) {
		Queue<BTBRole> roles = new PriorityQueue<>(DbManager.readTeamRolesForGuild(Long.parseLong(event.getGuild().getId())));

		BTBRole role = null;
		while ((role = roles.poll()) != null) {
			JDAManager.distributeTeamRoleToGuildTeamMembers(role);
		}

		JDAManager.sendMessage(event, "Team role distribution complete!");
	}

}
