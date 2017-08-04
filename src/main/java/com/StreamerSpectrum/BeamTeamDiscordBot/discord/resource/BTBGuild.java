package com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.StreamerSpectrum.BeamTeamDiscordBot.BTBMain;
import com.StreamerSpectrum.BeamTeamDiscordBot.Constants;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamChannel;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamUser;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeam;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeamUser;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.CommandHelper;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.MixerManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.ConstellationManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.DbManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.GuildManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.LFMManager;
import com.auth0.jwt.internal.org.apache.commons.lang3.StringUtils;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.PermissionException;

public class BTBGuild implements Comparable<BTBGuild> {
	private final static Logger		logger								= Logger.getLogger(BTBMain.class.getName());

	// DB Column Names
	public static final String		ID									= "ID";
	public static final String		Name								= "Name";
	public static final String		GoLiveChannelID						= "GoLiveChannelID";
	public static final String		LogChannelID						= "LogChannelID";
	public static final String		NewMemberChannelID					= "NewMemberChannelID";
	public static final String		AnnouncementChannelID				= "AnnouncementChannelID";
	public static final String		LFMChannelID						= "LFMChannelID";
	public static final String		GoLiveRoleID						= "GoLiveRoleID";
	public static final String		RemoveOfflineChannels				= "RemoveOfflineChannels";
	public static final String		UseHere								= "UseHere";

	public static final String[]	Columns								= { ID, Name, GoLiveChannelID, LogChannelID,
			NewMemberChannelID, AnnouncementChannelID, LFMChannelID, GoLiveRoleID, RemoveOfflineChannels, UseHere };

	private final long				id;

	private String					name;
	private String					goLiveChannelID						= null;
	private String					logChannelID						= null;
	private String					newMemberChannelID					= null;
	private String					announcementChannelID				= null;
	private String					lfmChannelID						= null;
	private String					goLiveRoleID						= null;

	private boolean					removeOfflineChannelAnnouncements	= false;
	private boolean					useHere								= false;

	public BTBGuild(long id) {
		this.id = id;
	}

	public BTBGuild(Map<String, String> values) {
		this.id = Long.parseLong(values.get(ID));

		this.name = values.get(Name);
		this.goLiveChannelID = values.get(GoLiveChannelID);
		this.logChannelID = values.get(LogChannelID);
		this.newMemberChannelID = values.get(NewMemberChannelID);
		this.announcementChannelID = values.get(AnnouncementChannelID);
		this.lfmChannelID = values.get(LFMChannelID);
		this.goLiveRoleID = values.get(GoLiveRoleID);
		this.removeOfflineChannelAnnouncements = "1".equals(values.get(RemoveOfflineChannels));
		this.useHere = "1".equals(values.get(UseHere));
	}

	@Override
	public int hashCode() {
		return new Long(id).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BTBGuild && ((BTBGuild) obj).id == this.id;
	}

	public long getID() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;

		update();
	}

	public String getGoLiveChannelID() {
		return goLiveChannelID;
	}

	public void setGoLiveChannelID(String goLiveChannelID) {
		this.goLiveChannelID = goLiveChannelID;

		if (StringUtils.isNotBlank(goLiveChannelID)) {
			Set<BTBBeamChannel> alreadyAnnounced = new HashSet<>();
			Queue<BeamTeam> teams = new PriorityQueue<>(getTrackedTeams());

			BeamTeam team = null;
			while ((team = teams.poll()) != null) {
				Queue<BeamTeamUser> members = new PriorityQueue<>(MixerManager.getTeamMembers(team));

				BeamTeamUser member = null;
				while ((member = members.poll()) != null) {
					if (member.channel.online && !alreadyAnnounced.contains(member.channel)) {
						BTBBeamChannel channel = MixerManager.getChannel(member.channel.id);

						MessageEmbed msg = JDAManager.buildGoLiveEmbed(channel);
						String textMsg = String.format("**%s is now live!** https://mixer.com/%s", channel.token,
								channel.token);

						sendGoLiveMessage(msg, textMsg, channel.id);

						alreadyAnnounced.add(channel);
					}
				}
			}

			Queue<BTBBeamChannel> channels = new PriorityQueue<>(getTrackedChannels());

			BTBBeamChannel channel = null;
			while ((channel = channels.poll()) != null) {
				if (!alreadyAnnounced.contains(channel)) {
					channel = DbManager.readChannel(channel.id);

					if (channel.online) {
						MessageEmbed msg = JDAManager.buildGoLiveEmbed(channel);
						String textMsg = String.format("**%s is now live!** https://mixer.com/%s", channel.token,
								channel.token);

						sendGoLiveMessage(msg, textMsg, channel.id);

						alreadyAnnounced.add(channel);
					}
				}
			}
		}

		update();
	}

	public String getLogChannelID() {
		return logChannelID;
	}

	public void setLogChannelID(String logChannelID) {
		this.logChannelID = logChannelID;

		update();
	}

	public String getNewMemberChannelID() {
		return newMemberChannelID;
	}

	public void setNewMemberChannelID(String newMemberChannelID) {
		this.newMemberChannelID = newMemberChannelID;

		update();
	}

	public String getAnnouncementChannelID() {
		return announcementChannelID;
	}

	public void setAnnouncementChannelID(String announcementChannelID) {
		this.announcementChannelID = announcementChannelID;

		update();
	}

	public String getGoLiveRoleID() {
		return goLiveRoleID;
	}

	public void setGoLiveRoleID(String goLiveRoleID) {
		this.goLiveRoleID = goLiveRoleID;

		update();
	}

	public boolean isRemoveOfflineChannelAnnouncements() {
		return removeOfflineChannelAnnouncements;
	}

	public void setRemoveOfflineChannelAnnouncements(boolean removeOfflineChannelAnnouncements) {
		this.removeOfflineChannelAnnouncements = removeOfflineChannelAnnouncements;

		update();
	}

	public boolean useHere() {
		return useHere;
	}

	public void setUseHere(boolean useHere) {
		this.useHere = useHere;

		update();
	}

	private void update() {
		Map<String, Object> where = new HashMap<>();
		where.put(BTBGuild.ID, id);

		DbManager.update(Constants.TABLE_GUILDS, getDbValues(), where);
	}

	public Map<String, Object> getDbValues() {
		Map<String, Object> values = new HashMap<>();

		values.put(ID, getID());
		values.put(Name, getName());
		values.put(GoLiveChannelID, getGoLiveChannelID());
		values.put(LogChannelID, getLogChannelID());
		values.put(NewMemberChannelID, getNewMemberChannelID());
		values.put(AnnouncementChannelID, getAnnouncementChannelID());
		values.put(LFMChannelID, getLFMChannelID());
		values.put(GoLiveRoleID, getGoLiveRoleID());
		values.put(RemoveOfflineChannels, isRemoveOfflineChannelAnnouncements() ? 1 : 0);
		values.put(UseHere, useHere() ? 1 : 0);

		return values;
	}

	public List<BeamTeam> getTrackedTeams() {
		return DbManager.readTrackedTeamsForGuild(id);
	}

	public List<BTBBeamChannel> getTrackedChannels() {
		return DbManager.readTrackedChannelsForGuild(id);
	}

	public boolean addTeam(BeamTeam team) {
		boolean added = false;

		if (DbManager.readTeam(team.id) == null) {
			DbManager.createTeam(team);
		}

		added = DbManager.createTrackedTeam(id, team.id);

		if (added) {
			if (StringUtils.isNotBlank(getGoLiveChannelID())) {
				Queue<BeamTeamUser> members = new PriorityQueue<>(MixerManager.getTeamMembers(team));

				BeamTeamUser member = null;
				while ((member = members.poll()) != null) {
					if (member.channel.online) {
						BTBBeamChannel channel = MixerManager.getChannel(member.channel.id);

						MessageEmbed msg = JDAManager.buildGoLiveEmbed(channel);
						String textMsg = String.format("**%s is now live!** https://mixer.com/%s", channel.token,
								channel.token);

						sendGoLiveMessage(msg, textMsg, channel.id);
					}
				}
			}

			ConstellationManager.subscribeToTeam(team);
		}

		return added;
	}

	public boolean removeTeam(BeamTeam team) {
		boolean isDeleted = false;

		isDeleted = DbManager.deleteTrackedTeam(id, team.id);

		return isDeleted;
	}

	public boolean addChannel(BTBBeamChannel channel) {
		boolean added = false;

		added = DbManager.createTrackedChannel(id, channel.id);

		if (added) {
			if (channel.online) {
				MessageEmbed msg = JDAManager.buildGoLiveEmbed(channel);
				String textMsg = String.format("**%s is now live!** https://mixer.com/%s", channel.token,
						channel.token);

				sendGoLiveMessage(msg, textMsg, channel.id);
			}
			ConstellationManager.subscribeToChannel(channel);
		}

		return added;
	}

	public boolean removeChannel(BTBBeamChannel channel) {
		boolean isDeleted = false;

		isDeleted = DbManager.deleteTrackedChannel(id, channel.id);

		return isDeleted;
	}

	public void sendGoLiveMessage(MessageEmbed embed, String text, int channelID) {
		if (StringUtils.isNotBlank(getGoLiveChannelID())) {
			JDA jda = JDAManager.getJDAForGuildID(getID());

			if (jda != null) {
				try {
					TextChannel goLiveChannel = jda.getTextChannelById(getGoLiveChannelID());

					if (null != goLiveChannel) {

						try {
							MessageBuilder embedBuilder = new MessageBuilder();

							if (useHere()) {
								embedBuilder.append("@here ");
							}

							embedBuilder.setEmbed(embed);

							goLiveChannel.sendMessage(embedBuilder.build()).queue(t -> {
								DbManager.createGoLiveMessage(
										new GoLiveMessage(t.getId(), getGoLiveChannelID(), getID(), channelID, null));
							});
						} catch (PermissionException ex) {
							if (ex.getPermission() == Permission.MESSAGE_EMBED_LINKS) {
								MessageBuilder textBuilder = new MessageBuilder();

								if (useHere()) {
									textBuilder.append("@here ");
								}

								textBuilder.append(text);

								goLiveChannel.sendMessage(textBuilder.build()).queue(t -> {
									DbManager.createGoLiveMessage(new GoLiveMessage(t.getId(), getGoLiveChannelID(),
											getID(), channelID, null));
								});;
							}
						}
					} else {
						setGoLiveChannelID(null);
					}
				} catch (PermissionException ex) {
					logger.log(Level.WARNING,
							String.format("Unable to send go live messages on %s's server!", getName()), ex);
				}
			}
		}

	}

	public void sendLogMessage(String text) {
		if (StringUtils.isNotBlank(getLogChannelID())) {
			JDAManager.sendMessage(getLogChannelID(),
					String.format("[%s] %s", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()), text));
		}
	}

	public void sendNewMemberMessage(BeamTeam team, BTBBeamUser member) {
		if (StringUtils.isNotBlank(getNewMemberChannelID())) {
			JDAManager.sendMessage(getNewMemberChannelID(), String.format(
					"@here, please give a warm welcome to %s's newest member, %s!", team.name, member.username));

			JDAManager.sendMessage(getNewMemberChannelID(),
					new EmbedBuilder().setTitle(member.username, String.format("https://mixer.com/%s", member.username))
							.setThumbnail(String.format("https://mixer.com/api/v1/users/%d/avatar?_=%d", member.id,
									new Random().nextInt()))
							.setDescription(StringUtils.isBlank(member.bio) ? "No bio" : member.bio)
							.addField("Followers", Integer.toString(member.channel.numFollowers), true)
							.addField("Views", Integer.toString(member.channel.viewersTotal), true)
							.addField("Partnered", member.channel.partnered ? "Yes" : "No", true)
							.addField("Joined Mixer", member.createdAt.toString(), true)
							.setImage(String.format("https://thumbs.mixer.com/channel/%d.small.jpg?_=%d",
									member.channel.id, new Random().nextInt()))
							.setFooter("Mixer.com", CommandHelper.BEAM_LOGO_URL).setTimestamp(Instant.now())
							.setColor(CommandHelper.COLOR).build());
		}
	}

	public void sendAnnouncement(String msg) {
		Guild guild = JDAManager.getGuildById(Long.toString(id));

		if (null != guild) {
			String channelID = null;
			MessageEmbed embed = new EmbedBuilder()
					.setTitle(String.format("%S ANNOUNCEMENT", JDAManager.getSelfUser().getName()), null)
					.setDescription(msg)
					.setFooter(JDAManager.getSelfUser().getName(), JDAManager.getSelfUser().getAvatarUrl())
					.setTimestamp(Instant.now()).setColor(new Color(195, 50, 50)).build();

			if (StringUtils.isNotBlank(getAnnouncementChannelID())) {
				channelID = getAnnouncementChannelID();
			} else if (StringUtils.isNotBlank(getLogChannelID())) {
				channelID = getLogChannelID();
			} else if (StringUtils.isNotBlank(getGoLiveChannelID())) {
				channelID = getGoLiveChannelID();
			} else if (StringUtils.isNotBlank(getNewMemberChannelID())) {
				channelID = getNewMemberChannelID();
			} else {
				List<TextChannel> channels = guild.getTextChannels();

				for (int i = 0; i < channels.size() && StringUtils.isBlank(channelID); ++i) {
					TextChannel channel = channels.get(i);

					if (channel.canTalk()) {
						channelID = channel.getId();
					}
				}
			}

			if (StringUtils.isNotBlank(channelID)) {
				JDAManager.sendMessage(channelID, embed);
			} else {
				JDAManager.sendDM(JDAManager.getGuildById(Long.toString(id)).getOwner().getUser(), embed);
			}
		}
	}

	@Override
	public int compareTo(BTBGuild other) {
		return Long.compare(this.id, other.id);
	}

	public void giveGoLiveRolesToAllLiveMembers() {
		if (isBotAllowedToDistributeLiveNow()) {
			Guild guild = JDAManager.getGuildById(Long.toString(getID()));

			Set<BTBBeamChannel> alreadyAnnounced = new HashSet<>();
			Queue<BeamTeam> teams = new PriorityQueue<>(getTrackedTeams());

			BeamTeam team = null;
			while ((team = teams.poll()) != null) {
				Queue<BeamTeamUser> members = new PriorityQueue<>(MixerManager.getTeamMembers(team));

				BeamTeamUser member = null;
				while ((member = members.poll()) != null) {
					if (member.channel.online && !alreadyAnnounced.contains(member.channel)) {
						BTBBeamChannel channel = MixerManager.getChannel(member.channel.id);

						if (null != member.social && StringUtils.isNotBlank(member.social.discord)) {
							User dcUser = JDAManager.getUserForDiscordTag(member.social.discord);

							if (null != dcUser) {
								Member dcMember = guild.getMember(dcUser);

								if (null != dcMember) {
									try {
										guild.getController()
												.addRolesToMember(dcMember, guild.getRoleById(getGoLiveRoleID()))
												.queue();
									} catch (PermissionException ex) {
										logger.log(Level.WARNING,
												String.format("Unable to modify permissions on %s's server.", name),
												ex);
									}
								}
							}
						}

						alreadyAnnounced.add(channel);
					}
				}
			}

			Queue<BTBBeamChannel> channels = new PriorityQueue<>(getTrackedChannels());

			BTBBeamChannel channel = null;
			while ((channel = channels.poll()) != null) {
				if (!alreadyAnnounced.contains(channel)) {
					if (channel != null && channel.online) {
						BTBBeamUser user = channel.user;

						if (null != user.social && StringUtils.isNotBlank(user.social.discord)) {
							User dcUser = JDAManager.getUserForDiscordTag(user.social.discord);

							if (null != dcUser) {
								Member dcMember = guild.getMember(dcUser);

								if (null != dcMember) {
									guild.getController()
											.addRolesToMember(dcMember, guild.getRoleById(getGoLiveRoleID())).queue();
								}
							}
						}

						alreadyAnnounced.add(channel);
					}
				}
			}
		}
	}

	public void giveGoLiveRoleTo(BTBBeamUser user) {
		if (null != user && isBotAllowedToDistributeLiveNow() && null != user.social
				&& StringUtils.isNotBlank(user.social.discord)) {
			Guild guild = JDAManager.getGuildById(Long.toString(id));
			User dcUser = JDAManager.getUserForDiscordTag(user.social.discord);

			if (null != dcUser) {
				Member dcMember = guild.getMember(dcUser);

				if (null != dcMember) {
					try {
						guild.getController().addRolesToMember(dcMember, guild.getRoleById(getGoLiveRoleID())).queue();
					} catch (PermissionException ex) {
						logger.log(Level.WARNING, String.format("Unable to modify permissions on %s's server.", name),
								ex);
					}
				}
			}
		}
	}

	public boolean isBotAllowedToDistributeLiveNow() {
		if (StringUtils.isNotBlank(getGoLiveRoleID())) {
			Guild guild = JDAManager.getGuildById(Long.toString(getID()));

			if (null != guild) {
				Role goLiveRole = guild.getRoleById(getGoLiveRoleID());

				if (null != goLiveRole) {
					for (Role role : guild.getMemberById(JDAManager.getSelfUser().getId()).getRoles()) {
						if (role.getPosition() > goLiveRole.getPosition()) {
							return true;
						}
					}

					logger.log(Level.INFO,
							String.format(
									"The guild \"%s\" does not have their permissions set correctly for the Live Now role.",
									guild.getName()));

					sendLogMessage(String.format(
							"@here I need one of my groups to be higher than %s to distribute it properly.",
							guild.getRoleById(getGoLiveRoleID()).getAsMention()));
				} else {
					GuildManager.getGuild(guild).setGoLiveRoleID(null);

					logger.log(Level.INFO,
							String.format("The guild \"%s\" has deleted their Live Now role.", guild.getName()));
				}

			}
		}

		return false;
	}

	public void removeGoLiveRolesFromAllMembers() {
		if (isBotAllowedToDistributeLiveNow()) {
			Guild guild = JDAManager.getGuildById(Long.toString(getID()));
			Role goLiveRole = guild.getRoleById(getGoLiveRoleID());

			List<Member> liveMembers = JDAManager.getGuildById(Long.toString(getID())).getMembersWithRoles(goLiveRole);

			for (Member member : liveMembers) {
				try {
					guild.getController().removeRolesFromMember(member, guild.getRoleById(getGoLiveRoleID())).queue();
				} catch (PermissionException ex) {
					logger.log(Level.WARNING, String.format("Unable to modify permissions on %s's server.", name), ex);
				}
			}
		}
	}

	public void removeGoLiveRoleFrom(BTBBeamUser user) {
		if (isBotAllowedToDistributeLiveNow() && null != user.social && StringUtils.isNotBlank(user.social.discord)) {
			Guild guild = JDAManager.getGuildById(Long.toString(id));
			User dcUser = JDAManager.getUserForDiscordTag(user.social.discord);

			if (null != dcUser) {
				Member dcMember = guild.getMember(dcUser);

				if (null != dcMember) {
					try {
						guild.getController().removeRolesFromMember(dcMember, guild.getRoleById(getGoLiveRoleID()))
								.queue();
					} catch (PermissionException ex) {
						logger.log(Level.WARNING, String.format("Unable to modify permissions on %s's server.", name),
								ex);
					}
				}
			}
		}
	}

	public List<GoLiveMessage> getGoLiveMessages() {
		List<GoLiveMessage> goLiveMessages = new ArrayList<>();

		Map<String, Object> where = new HashMap<>();
		where.put(GoLiveMessage.GUILD_ID, getID());

		List<Map<String, String>> valuesList = DbManager.read(Constants.TABLE_GOLIVEMESSAGES, null, null, where);

		for (Map<String, String> values : valuesList) {
			goLiveMessages.add(new GoLiveMessage(values));
		}

		return goLiveMessages;
	}

	public void refreshGoLiveMessage(MessageEmbed embed, GoLiveMessage message) {
		if (StringUtils.isNotBlank(getGoLiveChannelID()) && null != message) {
			JDA jda = JDAManager.getJDAForGuildID(getID());

			if (null != jda) {
				try {
					jda.getTextChannelById(message.getGoLiveChannelID()).getMessageById(message.getMessageID())
							.queue(t -> {
								MessageBuilder embedBuilder = new MessageBuilder();

								if (useHere()) {
									embedBuilder.append("@here ");
								}

								embedBuilder.setEmbed(embed);

								t.editMessage(embedBuilder.build()).queue();
							});
				} catch (NullPointerException ignore) {} catch (PermissionException e) {
					logger.log(Level.WARNING, String.format("Missing permission %s on server %s",
							e.getPermission().getName(), getName()));
				}
			}
		}
	}

	public String getLFMChannelID() {
		return lfmChannelID;
	}

	public void setLFMChannelID(String lfmChannelID) {
		this.lfmChannelID = lfmChannelID;

		update();
	}

	public void sendLFMMessage(long userID, long deleteTime, String msg) {
		if (StringUtils.isNotBlank(getLFMChannelID())) {
			JDA jda = JDAManager.getJDAForGuildID(getID());

			if (null != jda) {
				try {
					jda.getTextChannelById(getLFMChannelID()).sendMessage(msg).queue(m -> {
						DbManager.create(LFMManager.LFM_TABLE,
								new LFMMessage(userID, Long.parseLong(getLFMChannelID()), m.getIdLong(), deleteTime)
										.getDbValues());
					});
				} catch (NullPointerException ignore) {} catch (PermissionException e) {
					logger.log(Level.WARNING, String.format("Missing permission %s on server %s",
							e.getPermission().getName(), getName()));
				}
			}
		}

	}
}
