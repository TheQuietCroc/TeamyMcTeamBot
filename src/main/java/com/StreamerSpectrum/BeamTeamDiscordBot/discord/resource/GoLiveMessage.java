package com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource;

import java.util.HashMap;
import java.util.Map;

public class GoLiveMessage implements Comparable<GoLiveMessage> {

	// DB Column Names
	public static final String	ID					= "ID";
	public static final String	GO_LIVE_CHANNEL_ID	= "GoLiveChannelID";
	public static final String	GUILD_ID			= "GuildID";
	public static final String	CHANNEL_ID			= "ChannelID";

	private final String		messageID;
	private final String		goLiveChannelID;
	private final long			guildID;
	private final int			beamChannelID;

	public GoLiveMessage(String messageID, String goLiveChannelID, long guildID, int beamChannelID, String hereMsgID) {
		this.messageID = messageID;
		this.goLiveChannelID = goLiveChannelID;
		this.guildID = guildID;
		this.beamChannelID = beamChannelID;
	}

	public GoLiveMessage(Map<String, String> values) {
		this.messageID = values.get(ID);
		this.goLiveChannelID = values.get(GO_LIVE_CHANNEL_ID);
		this.guildID = Long.parseLong(values.get(GUILD_ID));
		this.beamChannelID = Integer.parseInt(values.get(CHANNEL_ID));
	}

	public String getMessageID() {
		return messageID;
	}

	public String getGoLiveChannelID() {
		return goLiveChannelID;
	}

	public long getGuildID() {
		return guildID;
	}

	public int getBeamChannelID() {
		return beamChannelID;
	}

	public Map<String, Object> getDbValues() {
		Map<String, Object> values = new HashMap<>();

		values.put(ID, messageID);
		values.put(GO_LIVE_CHANNEL_ID, goLiveChannelID);
		values.put(GUILD_ID, guildID);
		values.put(CHANNEL_ID, beamChannelID);

		return values;
	}

	@Override
	public int compareTo(GoLiveMessage other) {
		return Long.compare(Long.parseLong(this.messageID), Long.parseLong(other.messageID));
	}
}
