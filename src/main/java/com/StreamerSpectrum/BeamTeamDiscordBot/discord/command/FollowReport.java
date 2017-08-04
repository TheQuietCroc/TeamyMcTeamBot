package com.StreamerSpectrum.BeamTeamDiscordBot.discord.command;

import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.commons.lang3.StringUtils;

import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamChannel;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamUser;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeam;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeamUser;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.MixerManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;
import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;

public class FollowReport extends Command {

	public FollowReport() {
		this.name = "followreport";
		this.help = "Takes in a team name and a space-separated list of users and displays which members each user isn't following.";
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
				final List<BeamTeamUser> teamMembers = MixerManager.getTeamMembers(team);

				String userArg = null;
				while ((userArg = userArgs.poll()) != null) {
					StringBuilder followingList = new StringBuilder();
					StringBuilder notFollowingList = new StringBuilder();
					int followingCount = 0, notFollowingCount = 0;

					BTBBeamUser user = MixerManager.getUser(userArg);

					Queue<BTBBeamChannel> userFollows = new PriorityQueue<>(MixerManager.getFollowing(user.id));

					for (BeamTeamUser member : teamMembers) {
						if (!member.equals(user)) {
							boolean isNotFollowingMember = true;

							BTBBeamChannel followee = null;
							while ((followee = userFollows.poll()) != null) {
								if (member.channel.id == followee.id) {
									followingList.append(String.format("[%s](https://mixer.com/%s)\n", member.username,
											member.username));
									isNotFollowingMember = false;
									++followingCount;
									break;
								} else if (member.channel.id < followee.id) {
									break;
								}
							}

							if (isNotFollowingMember) {
								notFollowingList.append(
										String.format("[%s](https://mixer.com/%s)\n", member.username, member.username));
								++notFollowingCount;
							}
						}
					}

					if (followingCount == 0) {
						followingList.append("NONE");
					}

					if (notFollowingCount == 0) {
						notFollowingList.append("NONE");
					}

					CommandHelper.sendPaginationDM(event, followingList.toString().split("\n"), 1,
							"%s is Following %d/%d %s Members", user.username, followingCount, teamMembers.size(),
							team.name);

					CommandHelper.sendPaginationDM(event, notFollowingList.toString().split("\n"), 1,
							"%s is Not Following %d/%d %s Members", user.username, notFollowingCount,
							teamMembers.size(), team.name);
				}
			}
		} else {
			JDAManager.sendMessage(event, "Missing arguments from command!");
		}
	}

}
