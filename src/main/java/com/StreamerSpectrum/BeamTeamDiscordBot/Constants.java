package com.StreamerSpectrum.BeamTeamDiscordBot;

public abstract class Constants {
	public static final String	ADMIN_ID						= "180390479437889536";

	public static final String	INVITE_LINK						= "https://discordapp.com/oauth2/authorize?client_id=317667458938961921&scope=bot&permissions=269667409";

	// Database Tables
	public static final String	TABLE_CHANNELS					= "Channels";
	public static final String	TABLE_GOLIVEMESSAGES			= "GoLiveMessages";
	public static final String	TABLE_GUILDS					= "Guilds";
	public static final String	TABLE_TEAMROLES					= "TeamRoles";
	public static final String	TABLE_TEAMS						= "Teams";
	public static final String	TABLE_TRACKEDCHANNELS			= "TrackedChannels";
	public static final String	TABLE_TRACKEDTEAMS				= "TrackedTeams";
	public static final String	TABLE_VERSION					= "Version";

	// Database Columns

	public static final String	TRACKEDCHANNELS_COL_GUILDID		= "GuildID";
	public static final String	TRACKEDCHANNELS_COL_CHANNELID	= "ChannelID";

	public static final String	TRACKEDTEAMS_COL_GUILDID		= "GuildID";
	public static final String	TRACKEDTEAMS_COL_TEAMID			= "TeamID";
}
