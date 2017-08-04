package com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.configuration;

import java.time.Instant;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.BTBBeamChannel;
import com.StreamerSpectrum.BeamTeamDiscordBot.beam.resource.Costream;
import com.StreamerSpectrum.BeamTeamDiscordBot.discord.command.CommandHelper;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.MixerManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.GuildManager;
import com.StreamerSpectrum.BeamTeamDiscordBot.singletons.JDAManager;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.mixer.api.util.Enums;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.exceptions.PermissionException;

public class Test extends ConfigCommand {

	private static final BTBBeamChannel	testChannel	= MixerManager.getChannel(58717);
	private static final String			ARG_GO_LIVE	= "go_live";

	public Test() {
		super(String.format("%stest %s - Tests go-live announcements in the set go-live channel.\n",
				JDAManager.COMMAND_PREFIX, ARG_GO_LIVE));

		this.name = "test";
		this.help = String.format("use ***%s%s help*** for more info", JDAManager.COMMAND_PREFIX, this.name);
	}

	@Override
	protected void execute(CommandEvent event) {
		String[] args = event.getArgs().toLowerCase().split(" ");

		try {
			switch (args[0]) {
				case ARG_HELP:
					sendHelpMessage(event);
				break;
				case ARG_GO_LIVE:
					if (CommandHelper.canUseCommand(event, new Permission[] { Permission.MANAGE_CHANNEL }, null)) {
						testGoLive(event);
					}
				break;
				default:
					sendInvalidArgumentMessage(event, args[0]);
				break;
			}
		} catch (IndexOutOfBoundsException e) {
			JDAManager.sendMessage(event, "Unable to complete request, invalid arguments were passed to this command.");
		}

	}

	private void testGoLive(CommandEvent event) {
		final String channelID = GuildManager.getGuild(event.getGuild()).getGoLiveChannelID();

		if (StringUtils.isNotBlank(channelID)) {
			final JDA shard = event.getJDA();
			final String channelName = shard.getTextChannelById(channelID).getAsMention();

			try {
				final EmbedBuilder builder = new EmbedBuilder()
						.setAuthor(JDAManager.getSelfUser().getName(),
								String.format("https://mixer.com/%s", testChannel.token),
								JDAManager.getSelfUser().getAvatarUrl())
						.setTitle(String.format("%s is now live!", testChannel.token),
								String.format("https://mixer.com/%s", testChannel.token))
						.setThumbnail(testChannel.user.avatarUrl)
						.setDescription(StringUtils.isBlank(testChannel.user.bio) ? "No bio" : testChannel.user.bio)
						.addField(StringUtils.isBlank(testChannel.name) ? "Untitled Stream" : testChannel.name,
								testChannel.type == null || StringUtils.isBlank(testChannel.type.name)
										? "No game selected" : testChannel.type.name,
								false)
						.addField("Followers", Integer.toString(testChannel.numFollowers), true)
						.addField("Views", Integer.toString(testChannel.viewersTotal), true)
						.addField("Rating",
								null != testChannel.audience ? Enums.serializedName(testChannel.audience) : "Unrated",
								true)
						.setImage(String.format("https://thumbs.mixer.com/channel/%d.small.jpg?_=%d", testChannel.id,
								new Random().nextInt()))
						.setFooter("Mixer.com", CommandHelper.BEAM_LOGO_URL).setTimestamp(Instant.now())
						.setColor(CommandHelper.COLOR);

				Costream costream = MixerManager.getCostream(testChannel.costreamId);
				if (null != costream) {
					StringBuilder sb = new StringBuilder();

					for (BTBBeamChannel ch : costream.channels) {
						if (ch.id != testChannel.id) {
							sb.append(ch.token).append(", ");
						}
					}

					builder.addField("Costreaming With", sb.toString().substring(0, sb.lastIndexOf(",")), true);
				}

				// Test sending go-live embed
				shard.getTextChannelById(channelID).sendMessage(builder.build()).queue(msg -> {
					// Go-live embed post success
					event.getChannel().sendMessage("Successfully posted go-live embed to %s", channelName).queue();

					// Test editing go-live embed
					shard.getTextChannelById(channelID).editMessageById(msg.getId(), builder.build()).queue(edit -> {
						// Go-live embed edit success
						event.getChannel().sendMessage("Successfully edited go-live embed in %s", channelName).queue();

						// Test deleting go-live embed
						shard.getTextChannelById(channelID).deleteMessageById(edit.getId()).queue(Void -> {
							// Go-live embed delete success
							event.getChannel().sendMessage("Successfully deleted go-live embed in %s", channelName);
						}, t -> {
							// Go-live embed delete failure
							event.getChannel().sendMessage("Failed to delete go-live embed in %s.", channelName);
						});
					}, t -> {
						// Go-live embed post failure
						event.getChannel().sendMessage("Failed to edit go-live embed in %s", channelName).queue();

						// Test deleting go-live embed
						shard.getTextChannelById(channelID).deleteMessageById(msg.getId()).queue(Void -> {
							// Go-live embed delete success
							event.getChannel().sendMessage("Successfully deleted go-live embed in %s", channelName);
						}, e -> {
							// Go-live embed delete failure
							event.getChannel().sendMessage("Failed to delete go-live embed in %s.", channelName);
						});
					});

				}, t -> {
					// Go-live embed post failure
					event.getChannel().sendMessage("Failed to post go-live embed to %s", channelName).queue();
				});

				// Testing sending go-live text post
				shard.getTextChannelById(channelID).sendMessage("Test message").queue(msg -> {
					// Go-live text success
					event.getChannel().sendMessage("Successfully posted test message to %s", channelName).queue();

					// Testing deleting go-live text
					shard.getTextChannelById(channelID).deleteMessageById(msg.getId()).queue(Void -> {
						// Go-live text delete success
						event.getChannel().sendMessage("Successfully deleted test message in %s", channelName);
					}, t -> {
						// Go-live text delete failure
						event.getChannel().sendMessage("Failed to delete test message in %s.", channelName);
					});
				}, t -> {
					// Go-live text post failure
					event.getChannel().sendMessage("Failed to post test message to %s", channelName);
				});
			} catch (PermissionException e) {
				// Missing permissions failure
				event.getChannel().sendMessage("I'm missing the %s permission in the %s channel.",
						e.getPermission().getName(), channelName).queue();
			}
		} else {
			event.getChannel().sendMessage("No tests ran, there is no go-live channel for this server.").queue();
		}
	}

}
