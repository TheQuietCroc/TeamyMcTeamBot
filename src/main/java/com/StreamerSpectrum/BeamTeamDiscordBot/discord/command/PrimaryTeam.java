package com.StreamerSpectrum.BeamTeamDiscordBot.discord.command;

import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.commons.lang3.StringUtils;

import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeam;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeamUser;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.MixerManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;

public class PrimaryTeam extends Command {

	public PrimaryTeam() {
		this.name = "primaryteam";
		this.help = "Takes in a list of teams and displays details who on the team have it set as their primary and who doesn't.";
		this.arguments = "teamNamesOrIDs...";
	}

	@Override
	protected void execute(CommandEvent event) {
		if (!StringUtils.isBlank(event.getArgs())) {
			String[] args = event.getArgs().split(" ");

			for (String teamArg : args) {
				BeamTeam team = MixerManager.getTeam(teamArg);

				if (null != team) {
					int primaryTotal = 0, nonPrimaryTotal = 0;
					StringBuilder primaryMembers = new StringBuilder();
					StringBuilder nonPrimaryMembers = new StringBuilder();

					Queue<BeamTeamUser> members = new PriorityQueue<>(MixerManager.getTeamMembers(team));
					int memberSize = members.size();

					BeamTeamUser member = null;
					while ((member = members.poll()) != null) {
						if (member.primaryTeam != null && member.primaryTeam.intValue() == team.id) {
							primaryMembers.append(
									String.format("[%s](https://mixer.com/%s)\n", member.username, member.username));
							++primaryTotal;
						} else {
							nonPrimaryMembers.append(
									String.format("[%s](https://mixer.com/%s)\n", member.username, member.username));
							++nonPrimaryTotal;
						}
					}

					if (primaryTotal == 0) {
						primaryMembers.append("NONE");
					}

					if (nonPrimaryTotal == 0) {
						primaryMembers.append("NONE");
					}

					CommandHelper.sendPagination(event, primaryMembers.toString().split("\n"), 1,
							"%d/%d Members Have %s as Their Primary Team", primaryTotal, memberSize, team.name);

					CommandHelper.sendPagination(event, nonPrimaryMembers.toString().split("\n"), 1,
							"%d/%d Members Do Not Have %s as Their Primary Team", nonPrimaryTotal, memberSize,
							team.name);
				}
			}
		} else {
			JDAManager.sendMessage(event, "Missing arguments from command!");
		}
	}

}
