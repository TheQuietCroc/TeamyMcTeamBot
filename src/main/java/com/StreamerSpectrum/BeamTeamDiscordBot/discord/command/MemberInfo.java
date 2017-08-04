package com.StreamerSpectrum.BeamTeamDiscordBot.discord.command;

import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Queue;
import org.apache.commons.lang3.StringUtils;

import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeam;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeamUser;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.MixerManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;

public class MemberInfo extends Command {

	public MemberInfo() {
		this.name = "memberinfo";
		this.help = "Takes in a team name and a space-separated list of users and displays their info.";
		this.arguments = "teamNameOrID usernamesOrIDs...";
	}

	@Override
	protected void execute(CommandEvent event) {
		if (!StringUtils.isBlank(event.getArgs()) && event.getArgs().split(" ").length >= 2) {
			String teamArg = event.getArgs().split(" ")[0];
			Queue<String> userArgs = new PriorityQueue<>(
					Arrays.asList(event.getArgs().split(" ")).subList(1, event.getArgs().split(" ").length));

			BeamTeam team = MixerManager.getTeam(teamArg);

			if (null != team) {
				Queue<BeamTeamUser> teamMembers = new PriorityQueue<>(MixerManager.getTeamMembers(team));

				BeamTeamUser member = null;
				while (!userArgs.isEmpty() && (member = teamMembers.poll()) != null) {
					BeamTeamUser foundMember = null;

					for (String user : userArgs) {
						try {
							if (member.id == Integer.parseInt(user)) {
								foundMember = member;
								userArgs.remove(user);
								break;
							}
						} catch (NumberFormatException e) {
							if (StringUtils.equalsIgnoreCase(member.username, user)) {
								foundMember = member;
								userArgs.remove(user);
								break;
							}
						}
					}

					if (null != foundMember) {
						CommandHelper.sendTeamUserEmbed(event, foundMember);
					}
				}

				while (!userArgs.isEmpty()) {
					JDAManager.sendMessage(event,
							String.format("I cannot find the user %s in %s.", userArgs.poll(), team.name));
				}
			}
		} else {
			JDAManager.sendMessage(event, "Missing arguments from command!");
		}
	}
}
