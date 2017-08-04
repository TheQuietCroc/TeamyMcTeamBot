package com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import com.StreamerSpectrum.BeamTeamDiscordBot.BTBMain;
import com.StreamerSpectrum.BeamTeamDiscordBot.Constants;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeamUser;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.MixerManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.ConstellationManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.DbManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.GuildManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;

import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ReconnectedEvent;
import net.dv8tion.jda.core.events.ResumedEvent;
import net.dv8tion.jda.core.events.guild.GenericGuildEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.role.RoleDeleteEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class BTBListener implements EventListener {
	private final static Logger logger = Logger.getLogger(BTBMain.class.getName());

	@Override
	public void onEvent(Event event) {
		if (event instanceof ReconnectedEvent || event instanceof ResumedEvent) {
			ConstellationManager.start();
			logger.log(Level.INFO, "Constellation has been restarted due to a Discord reconnect.");
		} else if (event instanceof MessageDeleteEvent) {
			MessageDeleteEvent mde = ((MessageDeleteEvent) event);
			
			DbManager.deleteGoLiveMessage(mde.getMessageId());
		} else if (event instanceof GenericGuildEvent) {
			GenericGuildEvent gge = ((GenericGuildEvent) event);

			if (event instanceof GuildMemberJoinEvent) {
				GuildMemberJoinEvent gmje = ((GuildMemberJoinEvent) gge);

				Queue<BTBRole> roles = new PriorityQueue<>(
						DbManager.readTeamRolesForGuild(Long.parseLong(gmje.getGuild().getId())));

				BTBRole role = null;
				while ((role = roles.poll()) != null) {
					Queue<BeamTeamUser> teamMembers = new PriorityQueue<>(
							MixerManager.getTeamMembers(role.getTeamID()));

					BeamTeamUser member = null;
					while ((member = teamMembers.poll()) != null) {
						if (null != member.social && StringUtils.isNotBlank(member.social.discord)) {
							if (StringUtils.containsIgnoreCase(member.social.discord,
									gmje.getMember().getUser().getName())) {
								JDAManager.giveTeamRoleToUser(role, gmje.getMember().getUser());
								break;
							}
						}
					}
				}
			} else if (event instanceof GuildJoinEvent) {
				GuildManager.getGuild(gge.getGuild());
				logger.log(Level.INFO, String.format("%s has added the bot, they have been added to the database.",
						gge.getGuild().getName()));
			} else if (event instanceof GuildLeaveEvent) {
				// GuildManager.deleteGuild(Long.parseLong(gge.getGuild().getId()));
				// logger.log(Level.INFO, String.format(
				// "%s has removed the bot, they and all relevant records have
				// been purged from the database.",
				// gge.getGuild().getName()));
			}
		} else if (event instanceof RoleDeleteEvent) {
			Map<String, Object> where = new HashMap<>();
			where.put(BTBRole.RoleID, ((RoleDeleteEvent) event).getRole().getId());

			DbManager.delete(Constants.TABLE_TEAMROLES, where);
		}
	}

}
