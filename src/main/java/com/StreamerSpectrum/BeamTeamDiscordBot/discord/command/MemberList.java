package com.StreamerSpectrum.BeamTeamDiscordBot.discord.command;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.commons.lang3.StringUtils;

import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeam;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeamUser;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.MixerManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.GuildManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;

public class MemberList extends Command {

	public MemberList() {
		this.name = "memberlist";
		this.help = "Takes in a list of teams and displays a paginated list of each of their members.";
		this.arguments = "teamNamesOrIDs...";
	}

	@Override
	protected void execute(CommandEvent event) {
		if (!StringUtils.isBlank(event.getArgs())) {
			memberListHelper(event, event.getArgs().split(" "));
		} else if (!GuildManager.getGuild(event.getGuild()).getTrackedTeams().isEmpty()) {
			List<String> args = new ArrayList<>();
			Queue<BeamTeam> trackedTeams = new PriorityQueue<>(
					GuildManager.getGuild(event.getGuild()).getTrackedTeams());

			BeamTeam team = null;
			while ((team = trackedTeams.poll()) != null) {
				args.add(team.token);
			}

			memberListHelper(event, args.toArray(new String[] {}));
		} else {
			JDAManager.sendMessage(event, "Missing arguments from command!");
		}
	}

	private void memberListHelper(CommandEvent event, String[] args) {
		for (String teamArg : args) {
			BeamTeam team = MixerManager.getTeam(teamArg);

			if (null != team) {
				StringBuilder teamMembersSB = new StringBuilder();

				Queue<BeamTeamUser> members = new PriorityQueue<>(MixerManager.getTeamMembers(team));

				BeamTeamUser member = null;
				while ((member = members.poll()) != null) {
					teamMembersSB
							.append(String.format("[%s](https://mixer.com/%s)\n", member.username, member.username));
				}

				CommandHelper.sendPagination(event, teamMembersSB.toString().split("\n"), 1, "%s Has %d Members",
						team.name, members.size());
			}
		}
	}

}
