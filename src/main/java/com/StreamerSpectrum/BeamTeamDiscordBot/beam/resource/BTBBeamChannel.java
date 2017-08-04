package com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource;

import com.auth0.jwt.internal.org.apache.commons.lang3.StringUtils;
import com.google.gson.annotations.SerializedName;
import com.mixer.api.util.Enums;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class BTBBeamChannel implements Serializable, Comparable<BTBBeamChannel> {
	// DB Column Names
	public static final String	ID			= "ID";
	public static final String	TOKEN		= "Token";
	public static final String	USER_ID		= "UserID";
	public static final String	FOLLOWERS	= "Followers";
	public static final String	VIEWS		= "Views";
	public static final String	NAME		= "Name";
	public static final String	GAME		= "Game";
	public static final String	AUDIENCE	= "Audience";
	public static final String	BIO			= "Bio";
	public static final String	AVATAR_URL	= "AvatarURL";
	public static final String	DISCORD_TAG	= "DiscordTag";
	public static final String	COSTREAM_ID	= "CostreamID";
	public static final String	ONLINE		= "Online";

	public int					id;
	public int					userId;
	public String				token;
	public boolean				online;
	public boolean				partnered;
	public String				name;
	public AudienceRating		audience;
	public int					viewersTotal;
	public int					numFollowers;
	public boolean				interactive;
	public int					interactiveGameId;
	public int					ftl;
	public boolean				hasTranscodes;
	public Date					createdAt;
	public Type					type;
	public BTBBeamUser			user;
	public String				costreamId;

	public static class Type implements Serializable {
		public int		id;
		public String	name;
		public String	parent;
		public String	description;
		public String	source;
		public int		viewersCurrent;
		public int		online;
		public String	coverUrl;
	}

	public static enum CostreamPreference {
		@SerializedName("all")
		ALL, @SerializedName("following")
		FOLLOWING, @SerializedName("none")
		NONE
	}

	public static enum AudienceRating {
		@SerializedName("family")
		FAMILY, @SerializedName("teen")
		TEEN, @SerializedName("18+")
		ADULT
	}

	@Override
	public int hashCode() {
		return new Integer(id).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BTBBeamChannel && ((BTBBeamChannel) obj).id == this.id;
	}

	public BTBBeamChannel(Map<String, String> values) {
		this.id = Integer.parseInt(values.get(ID));
		this.token = values.get(TOKEN);
		this.userId = Integer.parseInt(values.get(USER_ID));
		this.numFollowers = Integer.parseInt(values.get(FOLLOWERS));
		this.viewersTotal = Integer.parseInt(values.get(VIEWS));
		this.name = values.get(NAME);
		this.type = new Type();
		this.type.name = values.get(GAME);
		if (values.get(AUDIENCE) != null) {
			switch (values.get(AUDIENCE)) {
				case "family":
					this.audience = AudienceRating.FAMILY;
				break;
				case "teen":
					this.audience = AudienceRating.TEEN;
				break;
				case "18+":
					this.audience = AudienceRating.ADULT;
				break;
				default:
				break;
			}
		}
		this.user = new BTBBeamUser();
		this.user.id = this.userId;
		this.user.username = this.token;
		this.user.bio = values.get(BIO);
		this.user.avatarUrl = values.get(AVATAR_URL);
		this.user.social = new Social();
		this.user.social.discord = values.get(DISCORD_TAG);
		this.costreamId = values.get(COSTREAM_ID);
		this.online = "1".equals(values.get(ONLINE));
	}

	public Map<String, Object> getDbValues() {
		Map<String, Object> values = new HashMap<>();

		values.put(ID, id);
		values.put(TOKEN, token);
		values.put(USER_ID, userId);
		values.put(FOLLOWERS, numFollowers);
		values.put(VIEWS, viewersTotal);
		if (StringUtils.isNotBlank(name)) {
			values.put(NAME, name);
		}
		if (null != type) {
			values.put(GAME, type.name);
		}
		if (null != audience) {
			values.put(AUDIENCE, Enums.serializedName(audience));
		}
		if (null != user && StringUtils.isNotBlank(user.bio)) {
			values.put(BIO, user.bio);
		}
		if (null != user && StringUtils.isNotBlank(user.avatarUrl)) {
			values.put(AVATAR_URL, user.avatarUrl);
		}
		if (null != user && null != user.social && null != user.social.discord) {
			values.put(DISCORD_TAG, user.social.discord);
		}
		values.put(COSTREAM_ID, costreamId);
		values.put(ONLINE, online ? "1" : "0");

		return values;
	}

	@Override
	public int compareTo(BTBBeamChannel other) {
		return Integer.compare(this.id, other.id);
	}

}
