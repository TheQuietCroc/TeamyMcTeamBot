package com.StreamerSpectrum.BeamTeamDiscordBot.singletons;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.StreamerSpectrum.BeamTeamDiscordBot.BTBMain;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamChannel;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamChannel.AudienceRating;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamChannel.Type;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamUser;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeam;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeamUser;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.Social;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource.BTBGuild;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource.GoLiveMessage;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mixer.api.resource.constellation.events.EventHandler;
import com.mixer.api.resource.constellation.events.LiveEvent;
import com.mixer.api.resource.constellation.methods.LiveSubscribeMethod;
import com.mixer.api.resource.constellation.methods.LiveUnsubscribeMethod;
import com.mixer.api.resource.constellation.methods.data.LiveRequestData;
import com.mixer.api.resource.constellation.replies.LiveRequestReply;
import com.mixer.api.resource.constellation.replies.ReplyHandler;
import com.mixer.api.resource.constellation.ws.MixerConstellationConnectable;

import net.dv8tion.jda.core.entities.MessageEmbed;

public abstract class ConstellationManager {
	private static final Logger						logger			= Logger.getLogger(BTBMain.class.getName());
	// private static Timer reconnectTimer;
	private static volatile boolean					isConnecting	= false;

	private static MixerConstellationConnectable	connectable;

	public static void start() {
		if (!isConnecting) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					isConnecting = true;
					// JDAManager.getSelfUser().getJDA().getPresence().setPresence(OnlineStatus.IDLE,
					// Game.of("Restarting Constellation..."));
					logger.log(Level.INFO, "Constellation start begin...");

					if (null != connectable && connectable.isOpen()) {
						connectable.disconnect();
					}

					connectable = MixerManager.getConstellation().connectable(MixerManager.getMixer());

					connectable.connect();

					// if (null == reconnectTimer) {
					// reconnectTimer = new Timer();
					//
					// reconnectTimer.scheduleAtFixedRate(new TimerTask() {
					//
					// @Override
					// public void run() {
					// logger.log(Level.INFO, "Constellation is
					// auto-restarting...");
					// start();
					// logger.log(Level.INFO, "Constellation auto-restart
					// complete!");
					// }
					// }, TimeUnit.MINUTES.toMillis(30),
					// TimeUnit.MINUTES.toMillis(30));
					// }

					init();

					isConnecting = false;
					logger.log(Level.INFO, "Constellation start complete!");
					// JDAManager.getSelfUser().getJDA().getPresence().setPresence(OnlineStatus.ONLINE,
					// Game.of("Type !tb help"));
				}
			}).run();
		} else {
			logger.log(Level.INFO, "Constellation startup in progress!");
		}
	}

	private static void init() {
		logger.log(Level.INFO, "Constellation init begin...");
		handleEvents();

		logger.log(Level.INFO, "Running startup announce");
		startupAnnounce();

		logger.log(Level.INFO, "Subscribing to announcements");
		subscribeToAnnouncements();
		logger.log(Level.INFO, "Subscribing to tracked teams");
		subscribeToAllTrackedTeams();
		logger.log(Level.INFO, "Subscribing to tracked channels");
		subscribeToAllTrackedChannels();

		// TODO: subscribe to tracked followers
		// TODO: subscribe to tracked followees
		logger.log(Level.INFO, "Constellation init complete!");
	}

	private static void startupAnnounce() {
		Set<Integer> teamIDs = new HashSet<>();
		Set<Integer> channelIDs = new HashSet<>();
		Set<Integer> alreadyAnnounced = new HashSet<>();

		{
			logger.log(Level.INFO, "Distributing go-live roles to members");
			List<BTBGuild> guilds = new ArrayList<>(GuildManager.getAllGuilds());

			for (BTBGuild guild : guilds) {
				guild.removeGoLiveRolesFromAllMembers();
				guild.giveGoLiveRolesToAllLiveMembers();
			}

			Queue<GoLiveMessage> messages = new PriorityQueue<>(DbManager.readAllGoLiveMessages());
			Map<Integer, Boolean> channelStatuses = new HashMap<>();

			// Check each go-live message stored in DB for channel live status
			for (GoLiveMessage message = null; (message = messages.poll()) != null;) {
				if (!channelStatuses.containsKey(message.getBeamChannelID())) {
					BTBBeamChannel channel = MixerManager.getChannelFromMixer(message.getBeamChannelID());

					if (null != channel && channel.online) {
						updateChannelAnnouncement(channel);
					} else {
						JDAManager.deleteGoLiveMessage(message);
					}

					alreadyAnnounced.add(message.getBeamChannelID());
					channelStatuses.put(channel.id, channel.online);
				} else if (!channelStatuses.get(message.getBeamChannelID())) {
					JDAManager.deleteGoLiveMessage(message);
				}
			}

			for (int channelID : channelStatuses.keySet()) {
				if (!channelStatuses.get(channelID)) {
					DbManager.deleteGoLiveMessagesForChannel(channelID);
				}
			}
		}

		{
			logger.log(Level.INFO, "Populating announce ids");
			List<GoLiveMessage> goLiveMessages = DbManager.readAllGoLiveMessages();

			for (GoLiveMessage msg : goLiveMessages) {
				if (!alreadyAnnounced.contains(msg.getBeamChannelID())) {
					alreadyAnnounced.add(msg.getBeamChannelID());
				}
			}

			List<BeamTeam> trackedTeams = DbManager.readAllTrackedTeams();

			for (BeamTeam team : trackedTeams) {
				if (!teamIDs.contains(team.id)) {
					teamIDs.add(team.id);
				}
			}

			List<BTBBeamChannel> trackedChannels = DbManager.readAllTrackedChannels();

			for (BTBBeamChannel channel : trackedChannels) {
				if (!alreadyAnnounced.contains(channel.id) && !channelIDs.contains(channel.id)) {
					channelIDs.add(channel.id);
				}
			}
		}

		logger.log(Level.INFO, "Announcing tracked team members");
		for (Integer teamID : teamIDs) {
			List<BeamTeamUser> members = MixerManager.getTeamMembers(teamID);

			for (BeamTeamUser member : members) {
				if (!alreadyAnnounced.contains(member.channel.id)) {

					if (member.channel.online) {
						List<BTBGuild> guilds = DbManager.readGuildsForTrackedTeam(teamID, true, false, false, false,
								false);
						BTBBeamChannel channel = MixerManager.getChannel(member.channel.id);
						MessageEmbed msg = JDAManager.buildGoLiveEmbed(channel);
						String textMsg = String.format("**%s is now live!** https://mixer.com/%s", channel.token,
								channel.token);

						for (BTBGuild guild : guilds) {
							guild.sendGoLiveMessage(msg, textMsg, channel.id);
						}
					}

					alreadyAnnounced.add(member.channel.id);
					channelIDs.remove(member.channel.id);
				}
			}
		}

		logger.log(Level.INFO, "Announcing tracked channels");
		for (Integer channelID : channelIDs) {
			if (!alreadyAnnounced.contains(channelID)) {
				BTBBeamChannel channel = MixerManager.getChannel(channelID);

				if (channel.online) {
					List<BTBGuild> guilds = DbManager.readGuildsForTrackedChannel(channelID, true, false, false, false,
							false);

					MessageEmbed msg = JDAManager.buildGoLiveEmbed(channel);
					String textMsg = String.format("**%s is now live!** https://mixer.com/%s", channel.token,
							channel.token);

					for (BTBGuild guild : guilds) {
						guild.sendGoLiveMessage(msg, textMsg, channel.id);
					}
				}

				alreadyAnnounced.add(channelID);
			}
		}
	}

	private static MixerConstellationConnectable getConnectable() {
		return connectable;
	}

	private static void handleEvents() {
		getConnectable().on(LiveEvent.class, new EventHandler<LiveEvent>() {

			@Override
			public void onEvent(LiveEvent event) {
				switch (getEventFromEvent(event)) {
					case "announcement:announce": {
						handleAnnouncements(event, event.data.payload);
					}
					break;
					case "channel:update": {
						handleChannelUpdate(event, event.data.payload);
					}
					break;
					case "channel:followed": {
						handleChannelFollowed(event, event.data.payload);
					}
					break;
					case "team:memberAccepted": {
						handleTeamMemberAccepted(event, event.data.payload);
					}
					break;
					case "team:memberInvited": {
						handleTeamMemberInvited(event, event.data.payload);
					}
					break;
					case "team:memberRemoved": {
						handleTeamMemberRemoved(event, event.data.payload);
					}
					break;
					case "team:ownerChanged": {
						handleTeamOwnerChanged(event, event.data.payload);
					}
					break;
					case "team:deleted": {
						handleTeamDeleted(event, event.data.payload);
					}
					break;
					case "user:update": {
						handleUserUpdate(event, event.data.payload);
					}
					break;
					case "user:followed": {
						handleUserFollowed(event, event.data.payload);
					}
					break;
					default:
						logger.log(Level.INFO,
								String.format("Unknown event: %s\n%s", event, event.data.payload.toString()));
						try {
							if (Files.notExists(Paths.get("payloads\\"))) {
								new File("payloads\\").mkdir();
							}

							Logger logger = Logger.getLogger(
									String.format("payload-%s", getEventFromEvent(event).replaceAll(":", "")));
							FileHandler fh = new FileHandler("payloads\\" + logger.getName() + ".json");
							SimpleFormatter formatter = new SimpleFormatter();
							fh.setFormatter(formatter);

							logger.addHandler(fh);

							logger.log(Level.INFO, event.data.payload.toString());
						} catch (SecurityException | IOException e) {}
					break;
				}
			}
		});
	}

	private static void handleAnnouncements(LiveEvent event, JsonObject payload) {
		try {
			if (Files.notExists(Paths.get("payloads\\"))) {
				new File("payloads\\").mkdir();
			}

			Logger logger = Logger.getLogger("payload-announcement");
			FileHandler fh = new FileHandler("payloads\\" + logger.getName() + ".json");
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);

			logger.addHandler(fh);

			logger.log(Level.INFO, payload.toString());
		} catch (SecurityException | IOException e) {}
	}

	private static void handleChannelUpdate(LiveEvent event, JsonObject payload) {
		BTBBeamChannel channel = MixerManager.getChannel(getIDFromEvent(event));

		boolean updateMsg = false;

		for (Entry<String, JsonElement> entry : payload.entrySet()) {
			switch (entry.getKey()) {
				case "audience": {
					updateMsg = true;
					switch (payload.get("audience").getAsString()) {
						case "family":
							channel.audience = AudienceRating.FAMILY;
						break;
						case "teen":
							channel.audience = AudienceRating.TEEN;
						break;
						case "18+":
							channel.audience = AudienceRating.ADULT;
						break;
						default:
						break;
					}
				}
				break;
				case "name": {
					updateMsg = true;
					channel.name = payload.get("name").getAsString();
				}
				break;
				case "type": {
					updateMsg = true;
					Type type = new Type();
					type.name = payload.get("type").getAsJsonObject().get("name").getAsString();

					channel.type = type;
				}
				break;
				case "viewersTotal": {
					channel.viewersTotal = payload.get("viewersTotal").getAsInt();

					DbManager.updateChannel(channel);
				}
				break;
				case "numFollowers": {
					channel.numFollowers = payload.get("numFollowers").getAsInt();

					DbManager.updateChannel(channel);
				}
				break;
				case "token": {
					updateMsg = true;
					channel.token = payload.get("token").getAsString();
				}
				break;
				case "costreamId": {
					updateMsg = true;
					channel.costreamId = payload.get("costreamId").getAsString();
				}
				break;
				case "online": {
					if (null != channel) {
						channel.online = payload.get("online").getAsBoolean();

						Set<BTBGuild> guilds = new HashSet<>();

						Queue<BeamTeam> userTeams = new PriorityQueue<>(MixerManager.getTeams(channel.userId));

						BeamTeam team = null;
						while ((team = userTeams.poll()) != null) {
							guilds.addAll(
									DbManager.readGuildsForTrackedTeam(team.id, true, false, false, false, false));
							guilds.addAll(
									DbManager.readGuildsForTrackedTeam(team.id, false, true, false, false, false));
							guilds.addAll(
									DbManager.readGuildsForTrackedTeam(team.id, false, false, false, false, true));
						}

						guilds.addAll(
								DbManager.readGuildsForTrackedChannel(channel.id, true, false, false, false, false));
						guilds.addAll(
								DbManager.readGuildsForTrackedChannel(channel.id, false, true, false, false, false));
						guilds.addAll(
								DbManager.readGuildsForTrackedChannel(channel.id, false, false, false, false, true));

						if (!guilds.isEmpty()) {
							if (payload.get("online").getAsBoolean()) {
								MessageEmbed msg = JDAManager.buildGoLiveEmbed(channel);
								String textMsg = String.format("**%s is now live!** https://mixer.com/%s",
										channel.token, channel.token);

								for (BTBGuild guild : guilds) {
									guild.sendGoLiveMessage(msg, textMsg, channel.id);
									guild.sendLogMessage(String.format("**%s** has gone ***live***!", channel.token));
									guild.giveGoLiveRoleTo(channel.user);
								}
							} else {
								Queue<GoLiveMessage> messagesList = new PriorityQueue<>(
										DbManager.readAllGoLiveMessagesForChannel(channel.id));

								GoLiveMessage message = null;
								while ((message = messagesList.poll()) != null) {
									JDAManager.deleteGoLiveMessage(message);
								}

								// TODO: delete this later
								DbManager.deleteGoLiveMessagesForChannel(channel.id);

								for (BTBGuild guild : guilds) {
									guild.sendLogMessage(
											String.format("**%s** has gone ***offline***.", channel.token));
									guild.removeGoLiveRoleFrom(channel.user);
								}
							}
						} else {
							logger.log(Level.INFO, String.format("No one is tracking %s's channel.", channel.token));
							unsubscribeFromEvent(event.data.channel);
						}
					}
				}
				break;
				case "thumbnailId":
				case "bannerUrl":
				case "vodsEnabled":
				case "coverId":
				case "cover":
				case "preferences":
				case "typeId":
				case "viewersCurrent":
				case "interactive":
				case "hostee":
				case "hosteeId":
				case "description":
				case "interactiveGameId":
				case "updatedAt":
				// Ignore these
				break;
				default:
					logger.log(Level.INFO,
							String.format("%s Channel Update payload: %s", channel.token, payload.toString()));
				break;
			}
		}

		if (updateMsg) {
			DbManager.updateChannel(channel);

			if (channel.online) {
				updateChannelAnnouncement(channel);
				logger.log(Level.INFO, String.format("Updated %s's announcement", channel.token));
			}
		}
	}

	private static void updateChannelAnnouncement(BTBBeamChannel channel) {
		MessageEmbed msgEmbed = JDAManager.buildGoLiveEmbed(channel);

		Queue<GoLiveMessage> messages = new PriorityQueue<>(DbManager.readAllGoLiveMessagesForChannel(channel.id));

		for (GoLiveMessage msg = messages.poll(); msg != null; msg = messages.poll()) {
			GuildManager.getGuild(msg.getGuildID()).refreshGoLiveMessage(msgEmbed, msg);
		}
	}

	private static void handleChannelFollowed(LiveEvent event, JsonObject payload) {
		try {
			if (Files.notExists(Paths.get("payloads\\"))) {
				new File("payloads\\").mkdir();
			}

			Logger logger = Logger.getLogger("payload-channelFollowed");
			FileHandler fh = new FileHandler("payloads\\" + logger.getName() + ".json");
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);

			logger.addHandler(fh);

			logger.log(Level.INFO, payload.toString());
		} catch (SecurityException | IOException e) {}
	}

	private static void handleTeamMemberAccepted(LiveEvent event, JsonObject payload) {
		Set<BTBGuild> guilds = new HashSet<>();
		guilds.addAll(DbManager.readGuildsForTrackedTeam(getIDFromEvent(event), false, true, false, false, false));
		guilds.addAll(DbManager.readGuildsForTrackedTeam(getIDFromEvent(event), false, false, true, false, false));

		BeamTeam team = DbManager.readTeam(getIDFromEvent(event));

		for (BTBGuild guild : guilds) {
			BTBBeamUser member = MixerManager.getUser(payload.get("id").getAsInt());

			guild.sendLogMessage(
					String.format("**%s** has joined ***%s***!", payload.get("username").getAsString(), team.token));
			guild.sendNewMemberMessage(team, member);
		}

		if (payload.has("social") && payload.get("social").getAsJsonObject().has("discord")) {
			JDAManager.giveTeamRoleToUserOnAllGuilds(getIDFromEvent(event), JDAManager
					.getUserForDiscordTag(payload.get("social").getAsJsonObject().get("discord").getAsString()));
		}

		subscribeToChannel(MixerManager.getUser(payload.get("id").getAsInt()).channel);
	}

	private static void handleTeamMemberInvited(LiveEvent event, JsonObject payload) {
		Queue<BTBGuild> guilds = new PriorityQueue<>(
				DbManager.readGuildsForTrackedTeam(getIDFromEvent(event), false, true, false, false, false));
		BeamTeam team = DbManager.readTeam(getIDFromEvent(event));

		BTBGuild guild = null;
		while ((guild = guilds.poll()) != null) {
			guild.sendLogMessage(String.format("**%s** has been invited to join ***%s***!",
					payload.get("username").getAsString(), team.token));
		}
	}

	private static void handleTeamMemberRemoved(LiveEvent event, JsonObject payload) {
		Queue<BTBGuild> guilds = new PriorityQueue<>(
				DbManager.readGuildsForTrackedTeam(getIDFromEvent(event), false, true, false, false, false));
		BeamTeam team = DbManager.readTeam(getIDFromEvent(event));

		BTBGuild guild = null;
		while ((guild = guilds.poll()) != null) {
			guild.sendLogMessage(
					String.format("**%s** has left ***%s***.", payload.get("username").getAsString(), team.name));
		}

		if (payload.has("social") && payload.get("social").getAsJsonObject().has("discord")) {
			JDAManager.removeTeamRoleFromUserOnAllGuilds(getIDFromEvent(event), JDAManager
					.getUserForDiscordTag(payload.get("social").getAsJsonObject().get("discord").getAsString()));

		}
	}

	private static void handleTeamOwnerChanged(LiveEvent event, JsonObject payload) {
		Queue<BTBGuild> guilds = new PriorityQueue<>(
				DbManager.readGuildsForTrackedTeam(getIDFromEvent(event), false, true, false, false, false));
		BeamTeam team = DbManager.readTeam(getIDFromEvent(event));

		BTBGuild guild = null;
		while ((guild = guilds.poll()) != null) {
			guild.sendLogMessage(String.format("**%s** owner changed to ***%s***.", team.name,
					payload.get("username").getAsString()));
		}
	}

	private static void handleTeamDeleted(LiveEvent event, JsonObject payload) {
		Queue<BTBGuild> guilds = new PriorityQueue<>(
				DbManager.readGuildsForTrackedTeam(getIDFromEvent(event), false, true, false, false, false));
		BeamTeam team = DbManager.readTeam(getIDFromEvent(event));

		BTBGuild guild = null;
		while ((guild = guilds.poll()) != null) {
			guild.sendLogMessage(String.format("**%s** has been deleted from Mixer.", team.name));
		}

		DbManager.deleteTeam(team.id);

		unsubscribeFromTeam(team);
	}

	private static void handleUserUpdate(LiveEvent event, JsonObject payload) {
		BTBBeamChannel channel = DbManager.readChannelForUserID(getIDFromEvent(event));

		if (null == channel) {
			BTBBeamUser user = MixerManager.getUser(getIDFromEvent(event));
			user.channel.user = user;
			channel = user.channel;
			DbManager.createChannel(user.channel);
		}

		boolean updateMsg = true;

		for (Entry<String, JsonElement> entry : payload.entrySet()) {
			switch (entry.getKey()) {
				case "avatarUrl": {
					channel.user.avatarUrl = payload.get("avatarUrl").getAsString();

					DbManager.updateChannel(channel);
				}
				break;
				case "bio": {
					channel.user.bio = payload.get("bio").getAsString();

					DbManager.updateChannel(channel);
				}
				break;
				case "social": {
					if (payload.get("social").getAsJsonObject().has("discord")) {

						if (null == channel.user.social) {
							channel.user.social = new Social();
						}

						channel.user.social.discord = payload.get("social").getAsJsonObject().get("discord")
								.getAsString();

						DbManager.updateChannel(channel);
					}
				}
				break;
				case "primaryTeam":
				case "preferences":
				case "sparks":
				case "experience":
				case "id":
					// Ignore these
					updateMsg = false;
				break;
				default:
					updateMsg = false;
					logger.log(Level.INFO, String.format("User Update payload: %s", payload.toString()));
				break;
			}
		}

		if (updateMsg) {
			MessageEmbed msgEmbed = JDAManager.buildGoLiveEmbed(channel);

			Queue<GoLiveMessage> messages = new PriorityQueue<>(
					DbManager.readAllGoLiveMessagesForChannel(getIDFromEvent(event)));

			for (GoLiveMessage msg = messages.poll(); msg != null; msg = messages.poll()) {
				GuildManager.getGuild(msg.getGuildID()).refreshGoLiveMessage(msgEmbed, msg);
			}
		}
	}

	private static void handleUserFollowed(LiveEvent event, JsonObject payload) {
		try {
			if (Files.notExists(Paths.get("payloads\\"))) {
				new File("payloads\\").mkdir();
			}

			Logger logger = Logger.getLogger("payload-userFollowed");
			FileHandler fh = new FileHandler("payloads\\" + logger.getName() + ".json");
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);

			logger.addHandler(fh);

			logger.log(Level.INFO, payload.toString());
		} catch (SecurityException | IOException e) {}
	}

	private static void subscribeToAnnouncements() {
		subscribeToEvent("announcement:announce");
	}

	public static void subscribeToChannel(BTBBeamChannel channel) {
		subscribeToEvent(String.format("channel:%d:update", channel.id));
	}

	public static void subscribeToChannelFollowers(int channelID) {
		subscribeToEvent(String.format("channel:%d:followed", channelID));
	}

	public static void subscribeToTeam(BeamTeam team) {
		if (DbManager.readTeam(team.id) == null) {
			DbManager.createTeam(team);
		}

		subscribeToEvent(String.format("team:%d:memberAccepted", team.id));
		subscribeToEvent(String.format("team:%d:memberInvited", team.id));
		subscribeToEvent(String.format("team:%d:memberRemoved", team.id));
		subscribeToEvent(String.format("team:%d:ownerChanged", team.id));
		subscribeToEvent(String.format("team:%d:deleted", team.id));

		Queue<BeamTeamUser> teamMembers = new PriorityQueue<>(MixerManager.getTeamMembers(team));

		BeamTeamUser member = null;
		while ((member = teamMembers.poll()) != null) {
			BTBBeamUser user = new BTBBeamUser();
			user.avatarUrl = member.avatarUrl;
			user.bio = member.bio;
			user.social = member.social;

			member.channel.user = user;
			subscribeToChannel(member.channel);
			subscribeToUser(member.id);
		}
	}

	private static void subscribeToAllTrackedTeams() {
		Set<BeamTeam> teams = new LinkedHashSet<>(DbManager.readAllTrackedTeams());

		for (BeamTeam team : teams) {
			subscribeToTeam(team);
		}
	}

	private static void subscribeToAllTrackedChannels() {
		Queue<BTBBeamChannel> channels = new PriorityQueue<>(DbManager.readAllTrackedChannels());

		BTBBeamChannel channel = null;
		while ((channel = channels.poll()) != null) {
			subscribeToChannel(channel);
			subscribeToUser(channel.userId);
		}
	}

	public static void unsubscribeFromTeam(BeamTeam team) {
		unsubscribeFromEvent(String.format("team:%d:memberAccepted", team.id));
		unsubscribeFromEvent(String.format("team:%d:memberInvited", team.id));
		unsubscribeFromEvent(String.format("team:%d:memberRemoved", team.id));
		unsubscribeFromEvent(String.format("team:%d:ownerChanged", team.id));
		unsubscribeFromEvent(String.format("team:%d:deleted", team.id));
	}

	public static void subscribeToUser(int userID) {
		subscribeToEvent(String.format("user:%d:update", userID));
		// subscribeToEvent(String.format("user:%d:followed", userID));
	}

	private static void subscribeToEvent(String event) {
		LiveSubscribeMethod lsm = new LiveSubscribeMethod();

		lsm.params = new LiveRequestData();
		lsm.params.events = new ArrayList<>();
		lsm.params.events.add(event);

		getConnectable().send(lsm, new ReplyHandler<LiveRequestReply>() {

			@Override
			public void onSuccess(LiveRequestReply result) {}
		});
	}

	private static void unsubscribeFromEvent(String event) {
		LiveUnsubscribeMethod lum = new LiveUnsubscribeMethod();

		lum.params = new LiveRequestData();
		lum.params.events = new ArrayList<>();
		lum.params.events.add(event);

		getConnectable().send(lum, new ReplyHandler<LiveRequestReply>() {

			@Override
			public void onSuccess(LiveRequestReply result) {}
		});
	}

	private static int getIDFromEvent(LiveEvent event) {
		return Integer.parseInt(
				event.data.channel.substring(event.data.channel.indexOf(":") + 1, event.data.channel.lastIndexOf(":")));
	}

	private static String getEventFromEvent(LiveEvent event) {
		return event.data.channel.replaceAll(":[0-9]*:", ":");
	}
}
