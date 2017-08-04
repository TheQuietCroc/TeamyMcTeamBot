package com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource;

import java.util.HashMap;
import java.util.Map;

public class LFMMessage implements Comparable<LFMMessage> {

	// DB Column Names
	public static final String	USER_ID		= "UserID";
	public static final String	CHANNEL_ID	= "ChannelID";
	public static final String	MESSAGE_ID	= "MessageID";
	public static final String	DELETE_TIME	= "DeleteTime";

	private final long			userID;
	private final long			channelID;
	private final long			messageID;
	private final long			deleteTime;

	public LFMMessage(long userID, long channelID, long messageID, long deleteTime) {
		this.userID = userID;
		this.channelID = channelID;
		this.messageID = messageID;
		this.deleteTime = deleteTime;
	}

	public LFMMessage(Map<String, String> entries) {
		this.userID = Long.parseLong(entries.get(LFMMessage.USER_ID));
		this.channelID = Long.parseLong(entries.get(LFMMessage.CHANNEL_ID));
		this.messageID = Long.parseLong(entries.get(LFMMessage.MESSAGE_ID));
		this.deleteTime = Long.parseLong(entries.get(LFMMessage.DELETE_TIME));
	}

	public long getUserID() {
		return userID;
	}

	public long getChannelID() {
		return channelID;
	}

	public long getMessageID() {
		return messageID;
	}

	public long getDeleteTime() {
		return deleteTime;
	}

	@Override
	public int compareTo(LFMMessage o) {
		return Long.compare(this.messageID, o.messageID);
	}

	public Map<String, Object> getDbValues() {
		Map<String, Object> values = new HashMap<>();

		values.put(USER_ID, userID);
		values.put(CHANNEL_ID, channelID);
		values.put(MESSAGE_ID, messageID);
		values.put(DELETE_TIME, deleteTime);

		return values;
	}

}
