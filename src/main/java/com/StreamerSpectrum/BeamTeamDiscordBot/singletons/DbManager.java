package com.StreamerSpectrum.BeamTeamDiscordBot.singletons;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.sqlite.SQLiteConfig;

import com.StreamerSpectrum.BeamTeamDiscordBot.BTBMain;
import com.StreamerSpectrum.BeamTeamDiscordBot.Constants;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamChannel;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BeamTeam;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource.BTBGuild;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource.BTBRole;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource.GoLiveMessage;

public abstract class DbManager {
	private final static Logger	logger	= Logger.getLogger(BTBMain.class.getName());

	private static Connection	connection;

	private static Connection getConnection() {
		if (null == connection) {
			try {
				File dbFile = new File("resources/bt.db");
				if (!dbFile.exists()) {
					FileUtils.copyFile(new File("resources/bt.db.template"), dbFile);
				}

				updateDb();

				SQLiteConfig config = new SQLiteConfig();
				config.enforceForeignKeys(true);
				connection = DriverManager.getConnection("jdbc:sqlite:resources/bt.db", config.toProperties());
			} catch (SQLException e) {
				logger.log(Level.SEVERE, "Unable to create database connection!", e);
			} catch (IOException e) {
				createDb();
			}
		}

		return connection;
	}

	private static void createDb() {
		// TODO: Fill this out
	}

	private static void updateDb() {
		// Add update scripts here
	}

	public static boolean create(String tableName, Map<String, Object> values) {
		if (StringUtils.isBlank(tableName)) {
			return false;
		}

		StringBuilder columns = new StringBuilder();
		StringBuilder vals = new StringBuilder();
		for (String key : values.keySet()) {
			columns.append(key).append(", ");
			vals.append("?, ");
		}

		PreparedStatement statement = null;

		try {
			try {
				statement = getConnection().prepareStatement(String.format("INSERT INTO %s (%s) VALUES (%s);",
						tableName, columns.substring(0, columns.lastIndexOf(",")),
						vals.substring(0, vals.lastIndexOf(","))));
				statement.setQueryTimeout(30);

				int i = 1;
				for (String key : values.keySet()) {
					statement.setObject(i++, values.get(key));
				}

				return statement.executeUpdate() > 0;
			} finally {
				if (null != statement) {
					statement.close();
				}
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE,
					String.format("An error occurred during SQL CREATE statement execution for statement: %s",
							statement.toString()),
					e);
		}

		return false;
	}

	public static List<Map<String, String>> read(String sql, List<String> args) {
		PreparedStatement statement = null;

		try {
			try {
				statement = getConnection().prepareStatement(sql);
				statement.setQueryTimeout(30);

				if (null != args) {
					int i = 1;
					for (String arg : args) {
						statement.setObject(i++, arg);
					}
				}

				ResultSet rs = statement.executeQuery();
				ResultSetMetaData rsmp = rs.getMetaData();
				List<Map<String, String>> values = new ArrayList<>();

				while (rs.next()) {
					Map<String, String> vals = new HashMap<>();

					for (int i = 1; i <= rsmp.getColumnCount(); ++i) {
						vals.put(rsmp.getColumnName(i), rs.getString(i));
					}

					values.add(vals);
				}

				return values;
			} finally {
				if (null != statement) {
					statement.close();
				}
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE,
					String.format("An error occurred during SQL SELECT statement execution for statement: %s",
							statement.toString()),
					e);
		}

		return new ArrayList<>();
	}

	public static List<Map<String, String>> read(String tableName, String[] columns, String innerJoin,
			Map<String, Object> where) {
		if (StringUtils.isBlank(tableName)) {
			return new ArrayList<>();
		}

		StringBuilder cols = new StringBuilder();
		if (null != columns && columns.length > 0) {
			for (String columnName : columns) {
				if (StringUtils.isNotBlank(columnName)) {
					cols.append(columnName).append(", ");
				}
			}
		}

		StringBuilder sql = new StringBuilder();
		sql.append(String.format("SELECT %s FROM %s",
				cols.length() > 0 ? cols.substring(0, cols.lastIndexOf(",")) : "*", tableName));

		if (StringUtils.isNotBlank(innerJoin)) {
			sql.append(String.format(" INNER JOIN %s", innerJoin));
		}

		if (null != where && !where.isEmpty()) {
			StringBuilder whereBuilder = new StringBuilder(" WHERE ");

			for (String key : where.keySet()) {
				if (StringUtils.contains(where.get(key).toString(), "NULL")) {
					whereBuilder.append(String.format("%s %s AND ", key, where.get(key)));
				} else {
					whereBuilder.append(String.format("%s = ? AND ", key));
				}
			}

			sql.append(String.format("%s", whereBuilder.substring(0, whereBuilder.lastIndexOf(" AND "))));
		}

		sql.append(";");

		PreparedStatement statement = null;

		try {
			try {
				statement = getConnection().prepareStatement(sql.toString());
				statement.setQueryTimeout(30);

				if (null != where && !where.isEmpty()) {
					int i = 1;
					for (String key : where.keySet()) {
						if (!StringUtils.contains(where.get(key).toString(), "NULL")) {
							statement.setObject(i++, where.get(key));
						}
					}
				}

				ResultSet rs = statement.executeQuery();
				ResultSetMetaData rsmp = rs.getMetaData();
				List<Map<String, String>> values = new ArrayList<>();

				while (rs.next()) {
					Map<String, String> vals = new HashMap<>();

					for (int i = 1; i <= rsmp.getColumnCount(); ++i) {
						vals.put(rsmp.getColumnName(i), rs.getString(i));
					}

					values.add(vals);
				}

				return values;
			} finally {
				if (null != statement) {
					statement.close();
				}
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE,
					String.format("An error occurred during SQL SELECT statement execution for statement: %s",
							statement.toString()),
					e);
		}

		return new ArrayList<>();
	}

	public static boolean update(String tableName, Map<String, Object> newVals, Map<String, Object> where) {
		if (StringUtils.isBlank(tableName) || null == newVals || newVals.isEmpty()) {
			return false;
		}

		StringBuilder sets = new StringBuilder();
		for (String key : newVals.keySet()) {
			sets.append(String.format("%s = ?, ", key));
		}

		StringBuilder sql = new StringBuilder();
		sql.append(String.format("UPDATE %s SET %s", tableName, sets.substring(0, sets.lastIndexOf(","))));

		if (null != where && !where.isEmpty()) {
			StringBuilder whereBuilder = new StringBuilder(" WHERE ");

			for (String key : where.keySet()) {
				if (StringUtils.contains(where.get(key).toString(), "NULL")) {
					whereBuilder.append(String.format("%s %s AND ", key, where.get(key)));
				} else {
					whereBuilder.append(String.format("%s = ? AND ", key));
				}
			}

			sql.append(String.format("%s", whereBuilder.substring(0, whereBuilder.lastIndexOf(" AND "))));
		}

		sql.append(";");

		PreparedStatement statement = null;

		try {
			try {
				statement = getConnection().prepareStatement(sql.toString());
				statement.setQueryTimeout(30);

				int i = 1;
				for (String key : newVals.keySet()) {
					statement.setObject(i++, newVals.get(key));
				}

				if (null != where && !where.isEmpty()) {
					for (String key : where.keySet()) {
						if (!StringUtils.contains(where.get(key).toString(), "NULL")) {
							statement.setObject(i++, where.get(key));
						}
					}
				}

				return statement.executeUpdate() > 0;
			} finally {
				if (null != statement) {
					statement.close();
				}
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE,
					String.format("An error occurred during SQL UPDATE statement execution for statement: %s",
							statement.toString()),
					e);
		}

		return false;
	}

	public static boolean delete(String tableName, Map<String, Object> where) {
		if (StringUtils.isBlank(tableName)) {
			return false;
		}

		StringBuilder sql = new StringBuilder(String.format("DELETE FROM %s", tableName));

		if (null != where && !where.isEmpty()) {
			StringBuilder whereBuilder = new StringBuilder(" WHERE ");

			for (String key : where.keySet()) {
				if (StringUtils.contains(where.get(key).toString(), "NULL")) {
					whereBuilder.append(String.format("%s %s AND ", key, where.get(key)));
				} else {
					whereBuilder.append(String.format("%s = ? AND ", key));
				}
			}

			sql.append(String.format("%s", whereBuilder.substring(0, whereBuilder.lastIndexOf(" AND "))));
		}

		sql.append(";");

		PreparedStatement statement = null;

		try {
			try {
				statement = getConnection().prepareStatement(sql.toString());
				statement.setQueryTimeout(30);

				if (null != where && !where.isEmpty()) {
					int i = 1;
					for (String key : where.keySet()) {
						if (!StringUtils.contains(where.get(key).toString(), "NULL")) {
							statement.setObject(i++, where.get(key));
						}
					}
				}

				return statement.executeUpdate() > 0;

			} finally {
				if (null != statement) {
					statement.close();
				}
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE,
					String.format("An error occurred during SQL DELETE statement execution for statement: %s",
							statement.toString()),
					e);
		}

		return false;
	}

	public static int readVersion() {
		return Integer.parseInt(read(Constants.TABLE_VERSION, new String[] { "ID" }, null, null).get(0).get(0));
	}

	public static boolean createChannel(BTBBeamChannel channel) {
		return create(Constants.TABLE_CHANNELS, channel.getDbValues());
	}

	public static BTBBeamChannel readChannel(int id) {
		Map<String, Object> where = new HashMap<>();
		where.put(BTBBeamChannel.ID, id);

		List<Map<String, String>> valuesList = read(Constants.TABLE_CHANNELS, null, null, where);

		BTBBeamChannel channel = null;

		if (!valuesList.isEmpty()) {
			Map<String, String> values = valuesList.get(0);
			if (!values.isEmpty()) {
				channel = new BTBBeamChannel(values);
			}
		}

		return channel;
	}

	public static BTBBeamChannel readChannel(String name) {
		Map<String, Object> where = new HashMap<>();
		where.put(BTBBeamChannel.TOKEN, name);

		List<Map<String, String>> valuesList = read(Constants.TABLE_CHANNELS, null, null, where);

		BTBBeamChannel channel = null;

		if (!valuesList.isEmpty()) {
			Map<String, String> values = valuesList.get(0);
			if (!values.isEmpty()) {
				channel = new BTBBeamChannel(values);
			}
		}

		return channel;
	}

	public static BTBBeamChannel readChannelForUserID(int id) {
		Map<String, Object> where = new HashMap<>();
		where.put(BTBBeamChannel.USER_ID, id);

		List<Map<String, String>> valuesList = read(Constants.TABLE_CHANNELS, null, null, where);

		BTBBeamChannel channel = null;

		if (!valuesList.isEmpty()) {
			Map<String, String> values = valuesList.get(0);
			if (!values.isEmpty()) {
				channel = new BTBBeamChannel(values);
			}
		}

		return channel;
	}

	public static List<BTBBeamChannel> readAllChannels() {
		List<BTBBeamChannel> channels = new ArrayList<>();
		List<Map<String, String>> valuesList = read(Constants.TABLE_CHANNELS, null, null, null);

		for (Map<String, String> values : valuesList) {
			BTBBeamChannel channel = new BTBBeamChannel(values);
			channels.add(channel);
		}

		return channels;
	}

	public static boolean updateChannel(BTBBeamChannel channel) {
		Map<String, Object> where = new HashMap<>();
		where.put(BTBBeamChannel.ID, channel.id);

		return update(Constants.TABLE_CHANNELS, channel.getDbValues(), where);
	}

	public static boolean deleteChannel(int id) {
		Map<String, Object> where = new HashMap<>();
		where.put(BTBBeamChannel.ID, id);

		return delete(Constants.TABLE_CHANNELS, where);
	}

	public static boolean createTeam(BeamTeam team) {
		return create(Constants.TABLE_TEAMS, team.getDbValues());
	}

	public static BeamTeam readTeam(int id) {
		Map<String, Object> where = new HashMap<>();
		where.put(BeamTeam.ID, id);

		List<Map<String, String>> valuesList = read(Constants.TABLE_TEAMS, null, null, where);

		BeamTeam team = null;
		if (!valuesList.isEmpty()) {
			Map<String, String> values = valuesList.get(0);

			if (!values.isEmpty()) {
				team = new BeamTeam();

				team.id = Integer.parseInt(values.get(BeamTeam.ID));
				team.ownerId = Integer.parseInt(values.get(BeamTeam.OWNER_ID));
				team.token = values.get(BeamTeam.TOKEN);
				team.name = values.get(BeamTeam.NAME);
			}
		}

		return team;
	}

	public static List<BeamTeam> readAllTeams() {
		List<BeamTeam> teams = new ArrayList<>();
		List<Map<String, String>> valuesList = read(Constants.TABLE_TEAMS, null, null, null);

		for (Map<String, String> values : valuesList) {
			BeamTeam team = new BeamTeam();

			team.id = Integer.parseInt(values.get(BeamTeam.ID));
			team.ownerId = Integer.parseInt(values.get(BeamTeam.OWNER_ID));
			team.token = values.get(BeamTeam.TOKEN);
			team.name = values.get(BeamTeam.NAME);

			teams.add(team);
		}

		return teams;
	}

	public static boolean updateTeam(BeamTeam team) {
		Map<String, Object> where = new HashMap<>();
		where.put(BeamTeam.ID, team.id);

		return update(Constants.TABLE_TEAMS, team.getDbValues(), where);
	}

	public static boolean deleteTeam(int id) {
		Map<String, Object> where = new HashMap<>();
		where.put(BeamTeam.ID, id);

		return delete(Constants.TABLE_TEAMS, where);
	}

	public static boolean createTrackedTeam(long guildID, int teamID) {
		Map<String, Object> values = new HashMap<>();

		values.put(Constants.TRACKEDTEAMS_COL_GUILDID, guildID);
		values.put(Constants.TRACKEDTEAMS_COL_TEAMID, teamID);

		return create(Constants.TABLE_TRACKEDTEAMS, values);
	}

	public static List<BeamTeam> readTrackedTeamsForGuild(long guildID) {
		List<BeamTeam> teams = new ArrayList<>();

		Map<String, Object> where = new HashMap<>();
		where.put(String.format("%s.%s", Constants.TABLE_TRACKEDTEAMS, Constants.TRACKEDTEAMS_COL_GUILDID), guildID);

		List<Map<String, String>> valueLists = read(Constants.TABLE_TEAMS, null,
				String.format("%s ON %s.%s = %s.%s", Constants.TABLE_TRACKEDTEAMS, Constants.TABLE_TEAMS, BeamTeam.ID,
						Constants.TABLE_TRACKEDTEAMS, Constants.TRACKEDTEAMS_COL_TEAMID),
				where);

		for (Map<String, String> values : valueLists) {
			BeamTeam team = new BeamTeam();

			team.id = Integer.parseInt(values.get(BeamTeam.ID));
			team.ownerId = Integer.parseInt(values.get(BeamTeam.OWNER_ID));
			team.token = values.get(BeamTeam.TOKEN);
			team.name = values.get(BeamTeam.NAME);

			teams.add(team);
		}

		return teams;
	}

	public static List<BTBGuild> readGuildsForTrackedTeam(int teamID) {
		return readGuildsForTrackedTeam(teamID, false, false, false, false, false);
	}

	public static List<BTBGuild> readGuildsForTrackedTeam(int teamID, boolean requireGoLive, boolean requireLogChannel,
			boolean requireNewMemberChannel, boolean requireAnnouncementChannel, boolean requireGoLiveRole) {
		List<BTBGuild> guilds = new ArrayList<>();
		Map<String, Object> where = new HashMap<>();

		where.put(String.format("%s.%s", Constants.TABLE_TRACKEDTEAMS, Constants.TRACKEDTEAMS_COL_TEAMID), teamID);

		if (requireGoLive) {
			where.put(String.format("%s.%s", Constants.TABLE_GUILDS, BTBGuild.GoLiveChannelID), "IS NOT NULL");
		}

		if (requireLogChannel) {
			where.put(String.format("%s.%s", Constants.TABLE_GUILDS, BTBGuild.LogChannelID), "IS NOT NULL");
		}

		if (requireNewMemberChannel) {
			where.put(String.format("%s.%s", Constants.TABLE_GUILDS, BTBGuild.NewMemberChannelID), "IS NOT NULL");
		}

		if (requireAnnouncementChannel) {
			where.put(String.format("%s.%s", Constants.TABLE_GUILDS, BTBGuild.AnnouncementChannelID), "IS NOT NULL");
		}

		if (requireGoLiveRole) {
			where.put(String.format("%s.%s", Constants.TABLE_GUILDS, BTBGuild.GoLiveRoleID), "IS NOT NULL");
		}

		List<Map<String, String>> valueLists = read(Constants.TABLE_GUILDS, null,
				String.format("%s ON %s.%s = %s.%s", Constants.TABLE_TRACKEDTEAMS, Constants.TABLE_GUILDS, BTBGuild.ID,
						Constants.TABLE_TRACKEDTEAMS, Constants.TRACKEDTEAMS_COL_GUILDID),
				where);

		for (Map<String, String> values : valueLists) {
			guilds.add(new BTBGuild(values));
		}

		return guilds;
	}

	public static List<BeamTeam> readAllTrackedTeams() {
		List<BeamTeam> teams = new ArrayList<>();

		List<Map<String, String>> valueLists = read(Constants.TABLE_TEAMS, null,
				String.format("%s ON %s.%s = %s.%s", Constants.TABLE_TRACKEDTEAMS, Constants.TABLE_TEAMS, BeamTeam.ID,
						Constants.TABLE_TRACKEDTEAMS, Constants.TRACKEDTEAMS_COL_TEAMID),
				null);

		for (Map<String, String> values : valueLists) {
			BeamTeam team = new BeamTeam();

			team.id = Integer.parseInt(values.get(BeamTeam.ID));
			team.ownerId = Integer.parseInt(values.get(BeamTeam.OWNER_ID));
			team.token = values.get(BeamTeam.TOKEN);
			team.name = values.get(BeamTeam.NAME);

			teams.add(team);
		}

		return teams;
	}

	public static boolean deleteTrackedTeam(long guildID, int teamID) {
		Map<String, Object> where = new HashMap<>();
		where.put(Constants.TRACKEDTEAMS_COL_GUILDID, guildID);
		where.put(Constants.TRACKEDTEAMS_COL_TEAMID, teamID);

		return delete(Constants.TABLE_TRACKEDTEAMS, where);
	}

	public static boolean createTrackedChannel(long guildID, int channelID) {
		Map<String, Object> values = new HashMap<>();

		values.put(Constants.TRACKEDCHANNELS_COL_GUILDID, guildID);
		values.put(Constants.TRACKEDCHANNELS_COL_CHANNELID, channelID);

		return create(Constants.TABLE_TRACKEDCHANNELS, values);
	}

	public static List<BTBBeamChannel> readTrackedChannelsForGuild(long guildID) {
		List<BTBBeamChannel> channels = new ArrayList<>();
		Map<String, Object> where = new HashMap<>();
		where.put(String.format("%s.%s", Constants.TABLE_TRACKEDCHANNELS, Constants.TRACKEDCHANNELS_COL_GUILDID),
				guildID);

		List<Map<String, String>> valueLists = read(Constants.TABLE_CHANNELS, null,
				String.format("%s ON %s.%s = %s.%s", Constants.TABLE_TRACKEDCHANNELS, Constants.TABLE_CHANNELS,
						BTBBeamChannel.ID, Constants.TABLE_TRACKEDCHANNELS, Constants.TRACKEDCHANNELS_COL_CHANNELID),
				where);

		for (Map<String, String> values : valueLists) {
			BTBBeamChannel channel = new BTBBeamChannel(values);
			channels.add(channel);
		}

		return channels;
	}

	public static List<BTBGuild> readGuildsForTrackedChannel(int channelID, boolean requireGoLive,
			boolean requireLogChannel, boolean requireNewMemberChannel, boolean requireAnnouncementChannel,
			boolean requireGoLiveRole) {
		List<BTBGuild> guilds = new ArrayList<>();

		Map<String, Object> where = new HashMap<>();
		where.put(String.format("%s.%s", Constants.TABLE_TRACKEDCHANNELS, Constants.TRACKEDCHANNELS_COL_CHANNELID),
				channelID);

		if (requireGoLive) {
			where.put(String.format("%s.%s", Constants.TABLE_GUILDS, BTBGuild.GoLiveChannelID), "IS NOT NULL");
		}

		if (requireLogChannel) {
			where.put(String.format("%s.%s", Constants.TABLE_GUILDS, BTBGuild.LogChannelID), "IS NOT NULL");
		}

		if (requireNewMemberChannel) {
			where.put(String.format("%s.%s", Constants.TABLE_GUILDS, BTBGuild.NewMemberChannelID), "IS NOT NULL");
		}

		if (requireAnnouncementChannel) {
			where.put(String.format("%s.%s", Constants.TABLE_GUILDS, BTBGuild.AnnouncementChannelID), "IS NOT NULL");
		}

		if (requireGoLiveRole) {
			where.put(String.format("%s.%s", Constants.TABLE_GUILDS, BTBGuild.GoLiveRoleID), "IS NOT NULL");
		}

		List<Map<String, String>> valueLists = read(Constants.TABLE_GUILDS, null,
				String.format("%s ON %s.%s = %s.%s", Constants.TABLE_TRACKEDCHANNELS, Constants.TABLE_GUILDS,
						BTBGuild.ID, Constants.TABLE_TRACKEDCHANNELS, Constants.TRACKEDCHANNELS_COL_GUILDID),
				where);

		for (Map<String, String> values : valueLists) {
			guilds.add(new BTBGuild(values));
		}

		return guilds;
	}

	public static List<BTBBeamChannel> readAllTrackedChannels() {
		List<BTBBeamChannel> channels = new ArrayList<>();

		List<Map<String, String>> valueLists = read(Constants.TABLE_CHANNELS, null,
				String.format("%s ON %s.%s = %s.%s", Constants.TABLE_TRACKEDCHANNELS, Constants.TABLE_CHANNELS,
						BTBBeamChannel.ID, Constants.TABLE_TRACKEDCHANNELS, Constants.TRACKEDCHANNELS_COL_CHANNELID),
				null);

		for (Map<String, String> values : valueLists) {
			BTBBeamChannel channel = new BTBBeamChannel(values);
			channels.add(channel);
		}

		return channels;
	}

	public static boolean deleteTrackedChannel(long guildID, int channelID) {
		Map<String, Object> where = new HashMap<>();
		where.put(Constants.TRACKEDCHANNELS_COL_GUILDID, guildID);
		where.put(Constants.TRACKEDCHANNELS_COL_CHANNELID, channelID);

		return delete(Constants.TABLE_TRACKEDCHANNELS, where);
	}

	public static boolean createGoLiveMessage(GoLiveMessage message) {
		return create(Constants.TABLE_GOLIVEMESSAGES, message.getDbValues());
	}

	public static List<GoLiveMessage> readAllGoLiveMessages() {
		List<GoLiveMessage> messages = new ArrayList<>();

		List<Map<String, String>> valueLists = read(Constants.TABLE_GOLIVEMESSAGES, null, null, null);

		for (Map<String, String> values : valueLists) {
			messages.add(new GoLiveMessage(values));
		}

		return messages;
	}

	public static List<GoLiveMessage> readAllGoLiveMessagesForChannel(int channelID) {
		List<GoLiveMessage> messages = new ArrayList<>();

		Map<String, Object> where = new HashMap<>();
		where.put(GoLiveMessage.CHANNEL_ID, channelID);

		List<Map<String, String>> valueLists = read(Constants.TABLE_GOLIVEMESSAGES, null, null, where);

		for (Map<String, String> values : valueLists) {
			messages.add(new GoLiveMessage(values));
		}

		return messages;
	}

	public static List<GoLiveMessage> readAllGoLiveMessagesForGuild(long guildID) {
		List<GoLiveMessage> messages = new ArrayList<>();

		Map<String, Object> where = new HashMap<>();
		where.put(GoLiveMessage.GUILD_ID, guildID);

		List<Map<String, String>> valueLists = read(Constants.TABLE_GOLIVEMESSAGES, null, null, where);

		for (Map<String, String> values : valueLists) {
			messages.add(new GoLiveMessage(values));
		}

		return messages;
	}

	public static boolean deleteAllGoLiveMessages() {
		return delete(Constants.TABLE_GOLIVEMESSAGES, null);
	}

	public static boolean deleteGoLiveMessagesForChannel(int channelID) {
		Map<String, Object> where = new HashMap<>();
		where.put(GoLiveMessage.CHANNEL_ID, channelID);

		return delete(Constants.TABLE_GOLIVEMESSAGES, where);
	}

	public static boolean deleteGoLiveMessage(String messageID) {
		Map<String, Object> where = new HashMap<>();
		where.put(GoLiveMessage.ID, messageID);

		return delete(Constants.TABLE_GOLIVEMESSAGES, where);
	}

	public static boolean createTeamRole(BTBRole role) {
		return create(Constants.TABLE_TEAMROLES, role.getDbValues());
	}

	public static List<BTBRole> readTeamRolesForGuild(long guildID) {
		List<BTBRole> roles = new ArrayList<>();
		Map<String, Object> where = new HashMap<>();
		where.put(BTBRole.GuildID, guildID);

		List<Map<String, String>> valuesList = read(Constants.TABLE_TEAMROLES, null, null, where);

		for (Map<String, String> values : valuesList) {
			roles.add(new BTBRole(values));
		}

		return roles;
	}

	public static List<BTBRole> readTeamRolesForTeam(int teamID) {
		List<BTBRole> roles = new ArrayList<>();
		Map<String, Object> where = new HashMap<>();
		where.put(BTBRole.TeamID, teamID);

		List<Map<String, String>> valuesList = read(Constants.TABLE_TEAMROLES, null, null, where);

		for (Map<String, String> values : valuesList) {
			roles.add(new BTBRole(values));
		}

		return roles;
	}

	public static boolean deleteTeamRole(BTBRole role) {
		return delete(Constants.TABLE_TEAMROLES, role.getDbValues());
	}

	public static boolean deleteTeamRole(long guildID, int teamID) {
		Map<String, Object> where = new HashMap<>();
		where.put(BTBRole.GuildID, guildID);
		where.put(BTBRole.TeamID, teamID);

		return delete(Constants.TABLE_TEAMROLES, where);
	}

	public static boolean deleteChannel(String name) {
		Map<String, Object> where = new HashMap<>();
		where.put(BTBBeamChannel.TOKEN, name);

		return delete(Constants.TABLE_CHANNELS, where);
	}

	public static boolean deleteTeam(String token) {
		Map<String, Object> where = new HashMap<>();
		where.put(BeamTeam.TOKEN, token);

		return delete(Constants.TABLE_TEAMS, where);
	}
}
