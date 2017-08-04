package com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.configuration;

import java.awt.Color;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.CommandHelper;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource.BTBGuild;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.GuildManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;
import com.jagrosh.jdautilities.commandclient.CommandEvent;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;

public class Use extends ConfigCommand {

	private static final String	ARG_GOLIVEDELETE	= "go_live_delete";
	private static final String	ARG_GOLIVEROLE		= "go_live_role";
	private static final String	ARG_GOLIVEHERE		= "go_live_here";

	public Use() {
		super(String.format(
				"%suse %s true/false - Turn on or off the deletion of go-live messages when the stream is offline.\n"
						+ "%suse %s true/false - Turn on or off the assigning of the \"Live Now\" role when streams go live.\n"
						+ "%suse %s true/false - Turn on or off the use of a @here tag when streams go live.\n",
				JDAManager.COMMAND_PREFIX, ARG_GOLIVEDELETE, 
				JDAManager.COMMAND_PREFIX, ARG_GOLIVEROLE,
				JDAManager.COMMAND_PREFIX, ARG_GOLIVEHERE));

		this.name = "use";
		this.help = String.format("use ***%s%s help*** for more info", JDAManager.COMMAND_PREFIX, this.name);
	}

	@Override
	protected void execute(CommandEvent event) {
		String[] args = event.getArgs().toLowerCase().split(" ");

		try {
			switch (args[0]) {
				case ARG_HELP:
					sendHelpMessage(event);
				break;
				case ARG_GOLIVEDELETE:
					if (CommandHelper.canUseCommand(event, new Permission[] { Permission.MANAGE_CHANNEL }, null)
							|| CommandHelper.canUseCommand(event, new Permission[] { Permission.MANAGE_ROLES }, null)) {
						useGoLiveDelete(event, Boolean.parseBoolean(args[1]));
					}
				break;
				case ARG_GOLIVEROLE:
					if (CommandHelper.canUseCommand(event, new Permission[] { Permission.MANAGE_ROLES }, null)) {
						useGoLiveRole(event, Boolean.parseBoolean(args[1]));
					}
				break;
				case ARG_GOLIVEHERE:
					if (CommandHelper.canUseCommand(event, new Permission[] { Permission.MANAGE_CHANNEL }, null)) {
						useHere(event, Boolean.parseBoolean(args[1]));
					}
				break;
				default:
					sendInvalidArgumentMessage(event, args[0]);
				break;
			}
		} catch (IndexOutOfBoundsException e) {
			JDAManager.sendMessage(event, "Unable to complete request, invalid arguments were passed to this command.");
		}
	}

	private void useHere(CommandEvent event, boolean isUse) {
		BTBGuild guild = GuildManager.getGuild(event.getGuild());

		guild.setUseHere(isUse);

		JDAManager.sendMessage(event, String.format("@here use for go-live messages set to %b", isUse));
	}

	private void useGoLiveDelete(CommandEvent event, boolean isUse) {
		BTBGuild guild = GuildManager.getGuild(event.getGuild());

		guild.setRemoveOfflineChannelAnnouncements(isUse);

		JDAManager.sendMessage(event, String.format("Offline go-live message removal set to %b", isUse));
	}

	private void useGoLiveRole(CommandEvent event, boolean isUse) {
		BTBGuild guild = GuildManager.getGuild(event.getGuild());

		if (isUse && StringUtils.isBlank(guild.getGoLiveRoleID())) {
			event.getGuild().getController().createRole().setName("Live Now").setColor(Color.YELLOW).setHoisted(true)
					.setMentionable(true).queue(new Consumer<Role>() {

						@Override
						public void accept(Role role) {
							guild.setGoLiveRoleID(role.getId());
							JDAManager.sendMessage(event, String.format("Live Now role use set to %b", isUse));
						}
					}, new Consumer<Throwable>() {

						@Override
						public void accept(Throwable t) {
							JDAManager.sendMessage(event, String
									.format("An error occurred while setting the Live Now role use to %b.", isUse));
						}
					});
		} else if (!isUse && !StringUtils.isBlank(guild.getGoLiveRoleID())) {
			event.getGuild().getRoleById(guild.getGoLiveRoleID()).delete().queue(new Consumer<Void>() {

				@Override
				public void accept(Void t) {
					guild.setGoLiveRoleID(null);
					JDAManager.sendMessage(event, String.format("Live Now role use set to %b", isUse));
				}

			}, new Consumer<Throwable>() {

				@Override
				public void accept(Throwable t) {
					JDAManager.sendMessage(event,
							String.format(
									"An error occurred while setting the Live Now role use to %b. Please ensure I have the MANAGE_ROLES permission.",
									isUse));
				}

			});
		} else {
			JDAManager.sendMessage(event, String.format("Live Now role use is already set to %b.", isUse));
		}
	}

}
