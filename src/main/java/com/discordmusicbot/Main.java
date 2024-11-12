package com.discordmusicbot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.discordmusicbot.lavaplayer.PlayerManager;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import static net.dv8tion.jda.api.interactions.commands.OptionType.BOOLEAN;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

public class Main extends ListenerAdapter {

	private static final String KEYPATH = "discordbotkey.txt";
	private static final String REFRESHTOKENPATH = "ytrefreshtoken.txt";

	private static String refreshToken;

	public static void main(String[] args) throws Exception {

		String token = Files.readAllLines(Paths.get(KEYPATH), StandardCharsets.UTF_8).get(0);

		try{
			refreshToken = Files.readAllLines(Paths.get(REFRESHTOKENPATH), StandardCharsets.UTF_8).get(0);
		} catch (Exception e) {
			refreshToken = null;
		}

		JDA jda = JDABuilder.createDefault(token)
		  .addEventListeners(new Main())
		  .build();

		PlayerManager.getInstance().load();

		CommandListUpdateAction commands = jda.updateCommands();

		commands.addCommands(
			Commands.slash("play", "Play song")
			  .addOption(STRING, "content", "Link or search", true)
		  )
		  .addCommands(Commands.slash("skip", "Skip song"))
		  .addCommands(Commands.slash("queue", "Queue"))
		  .addCommands(Commands.slash("pause", "Pause song"))
		  .addCommands(Commands.slash("resume", "Resume song"))
		  .addCommands(Commands.slash("clear", "Clear queue"))
		  .addCommands(Commands.slash("leave", "Leave voice channel"))
		  .addCommands(Commands.slash("join", "Join voice channel"))
		  .addCommands(Commands.slash("loop", "Loop")
			.addOptions(
			  new OptionData(STRING, "mode", "Loop mode", true)
				.addChoice("Track", "track")
				.addChoice("Queue", "queue")
				.addChoice("Off", "off")
			)
		  )
		  .addCommands(Commands.slash("remove", "Remove song from queue")
			.addOption(STRING, "index", "Index of song to remove", true)
		  )
		  .addCommands(Commands.slash("autoplay", "Turn autoplay on or off")
			.addOption(BOOLEAN, "boolean", "Turn autoplay on or off", true)
		  )
		  .queue();
		Thread overrideThread = new Thread(Main::overrideAllTracks);
		overrideThread.start();
	}

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		String command = event.getName();
		try{
			switch (command) {
				case "play":
					if(joinVoiceChannel(event)){
						String content = Objects.requireNonNull(event.getOption("content")).getAsString();
						PlayerManager.getInstance().loadAndPlay(event, content);
					}
					break;
				case "skip":
					if(!checkSameChannel(event)) {
						event.reply("Not in your voice channel").queue();
						return;
					}
					String nextTitle;
					try{
						nextTitle = PlayerManager.getInstance().getTrackScheduler(event).skipTrack();
					} catch (Exception e) {
						event.reply("No more tracks in queue").queue();
						return;
					}
					event.reply("Skipped to next track: " + nextTitle).queue();
					break;
				case "queue":
					if(!checkSameChannel(event)) {
						event.reply("Not in your voice channel").queue();
						return;
					}
					event.reply(PlayerManager.getInstance().getTrackScheduler(event).getQueue()).queue();
					break;
				case "pause":
					if(!checkSameChannel(event)) {
						event.reply("Not in your voice channel").queue();
						return;
					}
					PlayerManager.getInstance().getTrackScheduler(event).pauseTrack();
					event.reply("Paused track").queue();
					break;
				case "resume":
					if(!checkSameChannel(event)) {
						event.reply("Not in your voice channel").queue();
						return;
					}
					PlayerManager.getInstance().getTrackScheduler(event).resumeTrack();
					event.reply("Resumed track").queue();
					break;
				case "clear":
					if(!checkSameChannel(event)) {
						event.reply("Not in your voice channel").queue();
						return;
					}
					PlayerManager.getInstance().getTrackScheduler(event).clearQueue();
					event.reply("Cleared queue").queue();
					break;
				case "leave":
					if (checkSameChannel(event)) {
						PlayerManager.getInstance().deregisterPlayer(event.getGuild());
						event.getGuild().getAudioManager().closeAudioConnection();
						event.reply("Left voice channel").queue();
					} else {
						event.reply("Not in your voice channel").queue();
					}
					break;
				case "join":
					if(checkSameChannel(event)) {
						event.reply("Already in your voice channel").queue();
					} else {
						joinVoiceChannel(event);
						event.reply("Joined voice channel").queue();
					}
					break;
				case "loop":
					if(!checkSameChannel(event)) {
						event.reply("Not in your voice channel").queue();
					}
					event.reply(PlayerManager.getInstance().getTrackScheduler(event).setLoop(Objects.requireNonNull(event.getOption("mode")).getAsString())).queue();
					break;
				case "remove":
					if(!checkSameChannel(event)) {
						event.reply("Not in your voice channel").queue();
						return;
					}
					event.reply(PlayerManager.getInstance().getTrackScheduler(event).remove(Objects.requireNonNull(event.getOption("index")).getAsInt())).queue();
					break;
				case "autoplay":
					if(!checkSameChannel(event)) {
						event.reply("Not in your voice channel").queue();
						return;
					}
					boolean autoPlay = Objects.requireNonNull(event.getOption("boolean")).getAsBoolean();
					PlayerManager.getInstance().getTrackScheduler(event).setAutoPlay(autoPlay);
					event.reply("Autoplay is " + (autoPlay ? "on" : "off")).queue();
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean checkSameChannel(SlashCommandInteractionEvent event) {
		VoiceChannel userChannel = (VoiceChannel) event.getMember().getVoiceState().getChannel();
		VoiceChannel currentChannel = (VoiceChannel) event.getGuild().getAudioManager().getConnectedChannel();
		return userChannel != null && currentChannel != null && userChannel.getId().equals(currentChannel.getId());
	}

	private boolean joinVoiceChannel(SlashCommandInteractionEvent event) {
		VoiceChannel userChannel = (VoiceChannel) event.getMember().getVoiceState().getChannel();
		if(userChannel != null) {
			AudioManager audioManager = event.getGuild().getAudioManager();
			audioManager.openAudioConnection(userChannel);
			PlayerManager.getInstance().initPlayer(event.getGuild());
			return true;
		} else {
			return false;
		}
	}

	public static String getRefreshToken() {
		return refreshToken;
	}

	private static void overrideAllTracks(){
		while (true) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			try{
				String line = reader.readLine();
				System.out.println("Overriding: " + line);
				PlayerManager.getInstance().consoleOverride(line);
			} catch (Exception e) {
				System.out.println("Invalid input");
			}
		}
	}

	@Override
	public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
		Guild guild = event.getGuild();
		try{
			if(guild.getAudioManager().getConnectedChannel() == event.getChannelLeft()) {
				List<Member> members = guild.getAudioManager().getConnectedChannel().getMembers();
				int botCount = 0;
				for(Member member : members){
					if (member.getUser().isBot()) {
						botCount++;
					}
				}
				if (botCount == members.size()) {
					guild.getAudioManager().closeAudioConnection();
					PlayerManager.getInstance().deregisterPlayer(guild);
				}
			}
		} catch (Exception e) {
			// do nothing
		}
	}
}
