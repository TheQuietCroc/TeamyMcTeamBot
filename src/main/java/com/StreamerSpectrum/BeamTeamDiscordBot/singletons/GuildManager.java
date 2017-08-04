package com.StreamerSpectrum.BeamTeamDiscordBot.singletons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.StreamerSpectrum.BeamTeamDiscordBot.Constants;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.resource.BTBGuild;

import net.dv8tion.jda.core.entities.Guild;

public abstract class GuildManager {

	public static void addGuild(BTBGuild guild) {
		DbManager.create(Constants.TABLE_GUILDS, guild.getDbValues());
	}

	public static void deleteGuild(long id) {
		Map<String, Object> where = new HashMap<>();
		where.put(BTBGuild.ID, id);

		DbManager.delete(Constants.TABLE_GUILDS, where);
	}

	public static BTBGuild getGuild(long id) {

		Map<String, Object> where = new HashMap<>();
		where.put(BTBGuild.ID, id);

		List<Map<String, String>> values = DbManager.read(Constants.TABLE_GUILDS, null, null, where);

		BTBGuild guild = null;

		if (!values.isEmpty()) {
			guild = new BTBGuild(values.get(0));
		}

		return guild;
	}

	public static BTBGuild getGuild(Guild guild) {
		long id = Long.parseLong(guild.getId());
		BTBGuild storedGuild = getGuild(id);

		if (storedGuild == null) {
			storedGuild = new BTBGuild(id);
			storedGuild.setName(guild.getName());
			addGuild(storedGuild);
		}

		return storedGuild;
	}

	public static List<BTBGuild> getAllGuilds(String... requiredColumns) {
		Map<String, Object> where = new HashMap<>();

		if (null != requiredColumns) {
			for (String colName : requiredColumns) {
				if (Arrays.asList(BTBGuild.Columns).contains(colName)) {
					where.put(colName, "IS NOT NULL");
				}
			}
		}

		List<BTBGuild> guilds = new ArrayList<BTBGuild>();
		List<Map<String, String>> valuesList = DbManager.read(Constants.TABLE_GUILDS, null, null, where);

		for (Map<String, String> values : valuesList) {
			guilds.add(new BTBGuild(values));
		}

		return guilds;
	}
}
