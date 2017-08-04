package com.StreamerSpectrum.BeamTeamDiscordBot.discord.command;

import java.awt.Color;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.StreamerSpectrum.BeamTeamDiscordBot.Constants;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeamUser;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.MixerManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.jagrosh.jdautilities.menu.pagination.PaginatorBuilder;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.exceptions.PermissionException;

public abstract class CommandHelper {
	public static final Color	COLOR			= new Color(76, 144, 243);
	public static final String	BEAM_LOGO_URL	= "https://raw.githubusercontent.com/mixer/branding-kit/master/png/MixerMerge_Dark.png";

	public static boolean canUseCommand(CommandEvent event, Permission[] permissions, Role[] roles) {
		boolean canUseCommand = false;
		Member member = event.getMember();

		canUseCommand = isBotAdmin(member) || member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR)
				|| (permissions == null && roles == null) || (permissions != null && member.hasPermission(permissions))
				|| (roles != null && hasRoles(member, roles));

		if (!canUseCommand) {
			StringBuilder sb = new StringBuilder(
					String.format("%s, you do not have the requirements to use this command.", member.getAsMention()));

			if (permissions != null && !member.hasPermission(permissions)) {
				StringBuilder permissionBuilder = new StringBuilder(" You are missing the following permissions: ");

				for (Permission permission : permissions) {
					if (!member.hasPermission(permission)) {
						permissionBuilder.append(permission.getName()).append(", ");
					}
				}

				sb.append(permissionBuilder.substring(0, permissionBuilder.lastIndexOf(","))).append(".");
			}

			if (roles != null && !hasRoles(member, roles)) {
				StringBuilder roleBuilder = new StringBuilder(" You are missing one of the following roles: ");

				for (Role role : roles) {
					roleBuilder.append(role.getName()).append(", ");
				}

				sb.append(roleBuilder.substring(0, roleBuilder.lastIndexOf(","))).append(".");
			}

			JDAManager.sendMessage(event, sb.toString());
		}

		return canUseCommand;
	}

	public static boolean isBotAdmin(Member member) {
		return Constants.ADMIN_ID.equals(member.getUser().getId());
	}

	private static boolean hasRoles(Member member, Role[] roles) {
		for (Role role : roles) {
			if (member.getRoles().contains(role)) {
				return true;
			}
		}

		return false;
	}

	public static PaginatorBuilder buildPagination(CommandEvent event, String[] listItems, int numCols, String title) {
		return new PaginatorBuilder().setText(title).setItems(listItems)
				.setColumns(numCols < 1 ? 1 : numCols > 3 ? 3 : numCols).setFinalAction(m -> {
					try {
						m.clearReactions().queue();
					} catch (PermissionException e) {}
				}).setItemsPerPage(10).waitOnSinglePage(false).useNumberedItems(true).showPageNumbers(true)
				.setEventWaiter(JDAManager.getWaiter()).setTimeout(1, TimeUnit.MINUTES).setUsers(event.getAuthor())
				.setColor(CommandHelper.COLOR);
	}

	public static PaginatorBuilder buildPagination(CommandEvent event, String[] listItems, int numCols, String format,
			Object... args) {
		return buildPagination(event, listItems, numCols, String.format(format, args));
	}

	public static void sendPagination(CommandEvent event, String[] listItems, int numCols, String title) {
		sendPagination(event, buildPagination(event, listItems, numCols, title));
	}

	public static void sendPagination(CommandEvent event, String[] listItems, int numCols, String format,
			Object... args) {
		sendPagination(event, buildPagination(event, listItems, numCols, format, args));
	}

	public static void sendPagination(CommandEvent event, PaginatorBuilder builder) {
		builder.build().paginate(event.getChannel(), 0);
	}

	public static void sendPaginationDM(CommandEvent event, String[] listItems, int numCols, String title) {
		sendPaginationDM(event, buildPagination(event, listItems, numCols, title));
	}

	public static void sendPaginationDM(CommandEvent event, String[] listItems, int numCols, String format,
			Object... args) {
		sendPaginationDM(event, buildPagination(event, listItems, numCols, format, args));
	}

	public static void sendPaginationDM(CommandEvent event, PaginatorBuilder builder) {
		builder.build().paginate(event.getAuthor().openPrivateChannel().complete(), 0);
	}

	public static void sendTeamUserEmbed(CommandEvent event, BeamTeamUser member) {
		JDAManager.sendMessage(event,
				new EmbedBuilder().setTitle(member.username, String.format("https://mixer.com/%s", member.username))
						.setThumbnail(String.format("https://mixer.com/api/v1/users/%d/avatar?_=%d", member.id,
								new Random().nextInt()))
						.setDescription(StringUtils.isBlank(member.bio) ? "No bio" : member.bio)
						.addField("Followers", Integer.toString(member.channel.numFollowers), true)
						.addField("Views", Integer.toString(member.channel.viewersTotal), true)
						.addField("Partnered", member.channel.partnered ? "Yes" : "No", true)
						.addField("Primary Team", MixerManager.getTeam(member.primaryTeam).name, true)
						.addField("Joined Beam", member.createdAt.toString(), true)
						.addField("Member Since", member.teamMembership.createdAt.toString(), true)
						.setImage(String.format("https://thumbs.mixer.com/channel/%d.small.jpg?_=%d", member.channel.id,
								new Random().nextInt()))
						.setFooter("Mixer.com", BEAM_LOGO_URL).setTimestamp(Instant.now()).setColor(COLOR).build());
	}
}
