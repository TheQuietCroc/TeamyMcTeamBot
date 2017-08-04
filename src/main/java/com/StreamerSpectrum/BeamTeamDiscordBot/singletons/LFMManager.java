package com.StreamerSpectrum.BeamTeamDiscordBot.singletons;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource.BTBGuild;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource.LFMMessage;
import com.jagrosh.jdautilities.commandclient.CommandEvent;

public class LFMManager {

	public static final String	LFM_TABLE	= "LFMMessages";

	private static Timer		deleteTimer	= null;

	public static void init() {
		if (null == deleteTimer) {
			deleteTimer = new Timer();

			deleteTimer.scheduleAtFixedRate(new TimerTask() {

				@Override
				public void run() {
					deleteBroadcasts();
				}
			}, 0, TimeUnit.MINUTES.toMillis(1));
		}
	}

	public static void deleteBroadcasts() {
		List<Map<String, String>> results = DbManager.read(String.format("SELECT * FROM %s WHERE %s <= %d;", LFM_TABLE,
				LFMMessage.DELETE_TIME, new Date().getTime()), null);

		for (Map<String, String> result : results) {
			JDAManager.deleteLFMMessage(new LFMMessage(result));
		}
	}

	public static boolean cancelLFMForUser(long idLong) {
		List<LFMMessage> messages = getLFMMessagesForUserID(idLong);

		for (LFMMessage message : messages) {
			JDAManager.deleteLFMMessage(message);
		}

		return !messages.isEmpty();
	}

	private static List<LFMMessage> getLFMMessagesForUserID(long userID) {
		Map<String, Object> where = new HashMap<>();
		where.put(LFMMessage.USER_ID, userID);

		List<LFMMessage> messages = new ArrayList<>();

		for (Map<String, String> entries : DbManager.read(LFM_TABLE, null, null, where)) {
			messages.add(new LFMMessage(entries));
		}

		return messages;
	}

	public static boolean sendBroadcast(CommandEvent event, long deleteTime, String message) {
		boolean sent = false;
		if (getLFMMessagesForUserID(event.getAuthor().getIdLong()).isEmpty()) {
			List<BTBGuild> guilds = GuildManager.getAllGuilds(BTBGuild.LFMChannelID);
			for (BTBGuild guild : guilds) {
				guild.sendLFMMessage(event.getAuthor().getIdLong(), deleteTime,
						String.format("<%s>: %s", event.getAuthor().getAsMention(), message));
			}

			sent = true;
		}

		return sent;
	}

}
