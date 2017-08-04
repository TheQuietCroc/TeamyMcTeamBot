package com.StreamerSpectrum.BeamTeamDiscordBot.singletons;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;

import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.RandomMember;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.admin.*;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.configuration.*;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.teamrole.TeamRoleDistribute;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource.BTBListener;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource.BTBRole;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource.GoLiveMessage;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource.LFMMessage;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.MemberInfo;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.MemberList;
import com.StreamerSpectrum.BeamTeamDiscordBot.BTBMain;
import com.StreamerSpectrum.BeamTeamDiscordBot.Constants;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamChannel;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeamUser;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.Costream;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.CommandHelper;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.FollowReport;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.Invite;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.LFM;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.PrimaryTeam;
import com.jagrosh.jdautilities.commandclient.CommandClient;
import com.jagrosh.jdautilities.commandclient.CommandClientBuilder;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.jagrosh.jdautilities.waiter.EventWaiter;
import com.mixer.api.util.Enums;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.SelfUser;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

public abstract class JDAManager {
	public static final int			SHARD_TOTAL		= 3;
	private static final Logger		logger			= Logger.getLogger(BTBMain.class.getName());
	public static final String		COMMAND_PREFIX	= "!tb ";
	private static final String		SERVER_INVITE	= "https://discord.gg/uRwnz9g";

	private static List<JDA>		jdas;
	private static CommandClient	commandClient;
	private static EventWaiter		waiter;

	public static List<JDA> getJDAs() {
		if (null == jdas) {
			jdas = new ArrayList<>();

			try (BufferedReader br = new BufferedReader(new FileReader(new File("resources/config.txt")))) {
				String botToken = br.readLine();

				for (int i = 0; i < SHARD_TOTAL; ++i) {
					jdas.add(new JDABuilder(AccountType.BOT).useSharding(i, SHARD_TOTAL).setToken(botToken)
							.setStatus(OnlineStatus.DO_NOT_DISTURB).setGame(Game.of("loading..."))
							.addEventListener(getWaiter()).addEventListener(getCommandClient())
							.addEventListener(new BTBListener()).buildBlocking());
				}
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Missing resources/config.txt", e);
			} catch (InterruptedException | LoginException | IllegalArgumentException | RateLimitedException e) {
				logger.log(Level.SEVERE, "Unable to connect the bot to Discord!", e);
			}
		}

		return jdas;
	}

	public static JDA getJDAForGuildID(long guildID) {
		for (JDA j : JDAManager.getJDAs()) {
			if (j.getGuildById(guildID) != null) {
				return j;
			}
		}

		return null;
	}

	private static CommandClient getCommandClient() {
		if (null == commandClient) {
			commandClient = new CommandClientBuilder().useDefaultGame().setPrefix(COMMAND_PREFIX)
					.setOwnerId(Constants.ADMIN_ID)
					.addCommands(new Set(), new Remove(), new Track(), new Untrack(), new Show(), new Use(), new Test(),
							new LFM(), new RandomMember(), new PrimaryTeam(), new MemberInfo(), new FollowReport(),
							new MemberList(), new TeamRoleDistribute(), new Invite(), new SendAnnouncement(),
							new Restart())
					.setServerInvite(SERVER_INVITE).build();
		}

		return commandClient;
	}

	public static EventWaiter getWaiter() {
		if (null == waiter) {
			waiter = new EventWaiter();
		}

		return waiter;
	}

	public static void sendMessage(String channelID, MessageEmbed embed) {
		for (JDA jda : getJDAs()) {
			try {
				jda.getTextChannelById(channelID).sendMessage(embed).queue();
				break;
			} catch (NullPointerException ignore) {} catch (PermissionException ignore) {
				break;
			}
		}
	}

	public static void sendMessage(String channelID, String text) {
		for (JDA jda : getJDAs()) {
			try {
				jda.getTextChannelById(channelID).sendMessage(text).queue();
				break;
			} catch (NullPointerException ignore) {} catch (PermissionException ignore) {
				break;
			}
		}
	}

	public static void sendMessage(CommandEvent event, MessageEmbed embed) {
		sendMessage(event.getChannel().getId(), embed);
	}

	public static void sendMessage(CommandEvent event, String text) {
		sendMessage(event.getChannel().getId(), text);
	}

	public static void deleteGoLiveMessage(GoLiveMessage message) {
		if (GuildManager.getGuild(message.getGuildID()).isRemoveOfflineChannelAnnouncements()) {
			if (StringUtils.isNotBlank(message.getGoLiveChannelID())) {
				for (JDA jda : getJDAs()) {
					try {
						jda.getTextChannelById(message.getGoLiveChannelID()).deleteMessageById(message.getMessageID())
								.queue();
						break;
					} catch (NullPointerException ignore) {} catch (PermissionException ignore) {
						break;
					}
				}
			}
		}
	}

	public static void deleteMessage(String messageID, String goLiveChannelID) {
		if (StringUtils.isNotBlank(goLiveChannelID)) {
			for (JDA jda : getJDAs()) {
				try {
					jda.getTextChannelById(goLiveChannelID).deleteMessageById(messageID).queue(Void -> {}, t -> {
						logger.log(Level.INFO,
								String.format("Unable to delete message with ID %s from channel with ID %s", messageID,
										goLiveChannelID),
								t);
					});
					break;
				} catch (NullPointerException ignore) {} catch (PermissionException ignore) {
					break;
				}
			}
		}
	}

	public static void sendDM(CommandEvent event, MessageEmbed embed) {
		event.getAuthor().openPrivateChannel().complete().sendMessage(embed).queue();
	}

	public static void sendDM(CommandEvent event, String text) {
		event.getAuthor().openPrivateChannel().complete().sendMessage(text).queue();
	}

	public static void sendDM(User user, MessageEmbed embed) {
		user.openPrivateChannel().complete().sendMessage(embed).queue();
	}

	public static void sendDM(User user, String text) {
		user.openPrivateChannel().complete().sendMessage(text).queue();
	}

	public static void giveTeamRoleToUser(BTBRole role, User user) {
		if (null != user) {
			for (JDA jda : getJDAs()) {
				try {
					Guild guild = jda.getGuildById(Long.toString(role.getGuildID()));
					Member member = guild.getMember(user);

					if (null != member) {
						guild.getController().addRolesToMember(member, guild.getRoleById(role.getRoleID())).queue();
					}

					break;
				} catch (NullPointerException ignore) {} catch (PermissionException ignore) {
					break;
				}
			}
		}
	}

	public static void giveTeamRoleToUserOnAllGuilds(int teamID, User user) {
		Queue<BTBRole> roles = new PriorityQueue<>(DbManager.readTeamRolesForTeam(teamID));

		BTBRole role = null;
		while ((role = roles.poll()) != null) {
			giveTeamRoleToUser(role, user);
		}
	}

	public static void distributeTeamRoleToGuildTeamMembers(BTBRole role) {
		Queue<BeamTeamUser> members = new PriorityQueue<>(MixerManager.getTeamMembers(role.getTeamID()));

		BeamTeamUser member = null;
		while ((member = members.poll()) != null) {
			if (null != member.social && StringUtils.isNotBlank(member.social.discord)) {
				giveTeamRoleToUser(role, getUserForDiscordTag(member.social.discord));
			}
		}
	}

	public static void removeTeamRoleFromUser(BTBRole role, User user) {
		for (JDA jda : getJDAs()) {
			try {
				Guild guild = jda.getGuildById(Long.toString(role.getGuildID()));
				Member member = guild.getMember(user);

				if (null != member) {
					guild.getController().removeRolesFromMember(member, guild.getRoleById(role.getRoleID())).queue();
				}

				break;
			} catch (NullPointerException ignore) {} catch (PermissionException ignore) {
				break;
			}
		}
	}

	public static void removeTeamRoleFromUserOnAllGuilds(int teamID, User user) {
		Queue<BTBRole> roles = new PriorityQueue<>(DbManager.readTeamRolesForTeam(teamID));

		BTBRole role = null;
		while ((role = roles.poll()) != null) {
			removeTeamRoleFromUser(role, user);
		}
	}

	public static User getUserForDiscordTag(String tag) {
		for (JDA jda : getJDAs()) {
			try {
				List<User> potentialUsers = jda.getUsersByName(tag.substring(0, tag.indexOf("#")), true);

				for (User user : potentialUsers) {
					if (StringUtils.contains(tag, user.getDiscriminator())) {
						return user;
					}
				}
			} catch (NullPointerException ignore) {} catch (PermissionException ignore) {
				break;
			}
		}

		return null;
	}

	public static Guild getGuildById(String id) {
		for (JDA jda : getJDAs()) {
			try {
				if (jda.getGuildById(id) != null) {
					return jda.getGuildById(id);
				}
			} catch (NullPointerException ignore) {}
		}

		return null;
	}

	public static SelfUser getSelfUser() {
		return getJDAs().get(0).getSelfUser();
	}

	public static MessageEmbed buildGoLiveEmbed(BTBBeamChannel channel) {
		EmbedBuilder builder = new EmbedBuilder()
				.setAuthor(JDAManager.getSelfUser().getName(), String.format("https://mixer.com/%s", channel.token),
						JDAManager.getSelfUser().getAvatarUrl())
				.setTitle(String.format("%s is now live!", channel.token),
						String.format("https://mixer.com/%s", channel.token))
				.setThumbnail(channel.user.avatarUrl)
				.setDescription(StringUtils.isBlank(channel.user.bio) ? "No bio" : channel.user.bio)
				.addField(StringUtils.isBlank(channel.name) ? "Untitled Stream" : channel.name,
						channel.type == null || StringUtils.isBlank(channel.type.name) ? "No game selected"
								: channel.type.name,
						false)
				.addField("Followers", Integer.toString(channel.numFollowers), true)
				.addField("Views", Integer.toString(channel.viewersTotal), true)
				.addField("Rating", null != channel.audience ? Enums.serializedName(channel.audience) : "Unrated", true)
				.setImage(String.format("https://thumbs.mixer.com/channel/%d.small.jpg?_=%d", channel.id,
						new Random().nextInt()))
				.setFooter("Mixer.com", CommandHelper.BEAM_LOGO_URL).setTimestamp(Instant.now())
				.setColor(CommandHelper.COLOR);

		Costream costream = MixerManager.getCostream(channel.costreamId);
		if (null != costream) {
			StringBuilder sb = new StringBuilder();

			for (BTBBeamChannel ch : costream.channels) {
				if (ch.id != channel.id) {
					sb.append(ch.token).append(", ");
				}
			}

			builder.addField("Costreaming With", sb.toString().substring(0, sb.lastIndexOf(",")), true);
		}

		return builder.build();
	}

	public static void deleteLFMMessage(LFMMessage message) {
		try {
			for (JDA jda : getJDAs()) {
				jda.getTextChannelById(message.getChannelID()).deleteMessageById(message.getMessageID()).queue(Void -> {
					DbManager.delete(LFMManager.LFM_TABLE, message.getDbValues());
				});
			}
		} catch (Exception ignore) {}

	}
}
