package com.discordmusicbot.lavaplayer;

import com.discordmusicbot.lavaplayer.resulthandlers.AutoPlayResultHandler;
import com.discordmusicbot.lavaplayer.resulthandlers.OverrideResultHandler;
import com.discordmusicbot.lavaplayer.resulthandlers.SearchResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import static com.discordmusicbot.Main.getRefreshToken;

public class PlayerManager {
	private static final PlayerManager INSTANCE = new PlayerManager();
	private final Map<Long, GuildMusicManager> musicManagers;
	private final AudioPlayerManager playerManager;

	private PlayerManager() {
		this.musicManagers = new HashMap<>();
		this.playerManager = new DefaultAudioPlayerManager();

		YoutubeAudioSourceManager ytSourceManager = new dev.lavalink.youtube.YoutubeAudioSourceManager(/*allowSearch:*/ true, true, false);

		String refreshToken = getRefreshToken();
		if(refreshToken == null) {
			getNewToken(ytSourceManager);
		} else {
			try{
				ytSourceManager.useOauth2(refreshToken, false);
			} catch (Exception e) {
				getNewToken(ytSourceManager);
			}
		}

		ytSourceManager.getOauth2RefreshToken();

		playerManager.registerSourceManager(ytSourceManager);
		AudioSourceManagers.registerRemoteSources(playerManager,
		  com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class);
		AudioSourceManagers.registerLocalSource(playerManager);
	}

	private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
		long guildId = Long.parseLong(guild.getId());
		GuildMusicManager musicManager = musicManagers.get(guildId);

		if (musicManager == null) {
			musicManager = new GuildMusicManager(playerManager, guild);
			musicManagers.put(guildId, musicManager);
		}

		guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

		return musicManager;
	}

	public static PlayerManager getInstance() {
		return INSTANCE;
	}

	public void loadAndPlay(SlashCommandInteractionEvent event, String trackUrl) {
		GuildMusicManager musicManager = getGuildAudioPlayer(event.getGuild());
		//check if url is a valid url
		if (!trackUrl.startsWith("http")) {
			trackUrl = "ytsearch:" + trackUrl;
		}
		playerManager.loadItemOrdered(musicManager, trackUrl, new SearchResultHandler(event, musicManager, trackUrl));
	}

	public void autoPlay(Guild guild, String artistName) {
		GuildMusicManager musicManager = getGuildAudioPlayer(guild);
		playerManager.loadItemOrdered(musicManager, "ytsearch:" + artistName + " music", new AutoPlayResultHandler(musicManager));
	}

	public TrackScheduler getTrackScheduler(SlashCommandInteractionEvent event) {
		GuildMusicManager musicManager = getGuildAudioPlayer(event.getGuild());
		return musicManager.scheduler;
	}

	public void initPlayer(Guild guild) {
		getGuildAudioPlayer(guild);
	}

	public void deregisterPlayer(Guild guild) {
		musicManagers.remove(Long.parseLong(guild.getId()));
	}

	public void load(){
		// prevent lazy loading
	}

	private void getNewToken(YoutubeAudioSourceManager ytSourceManager){
		ytSourceManager.useOauth2(null, false);
		Thread tokenThread = new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				ytSourceManager.getOauth2RefreshToken();
				if(ytSourceManager.getOauth2RefreshToken() != null) {
					File file = new File("ytrefreshtoken.txt");
					FileWriter writer;
					try {
						writer = new FileWriter(file, false);
						writer.write(ytSourceManager.getOauth2RefreshToken());
						writer.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				}
			}
		});
		tokenThread.start();
	}

	public void consoleOverride(String trackUrl) {
		if(musicManagers.isEmpty()) {
			return;
		}
		if (!trackUrl.startsWith("http")) {
			trackUrl = "ytsearch:" + trackUrl;
		}
		playerManager.loadItemOrdered(musicManagers.values().stream().findFirst().get(), trackUrl, new OverrideResultHandler());
	}

	public void overrideAllPlayers(AudioTrack track) {
		for(GuildMusicManager musicManager : musicManagers.values()) {
			musicManager.scheduler.overridePlayingTrack(track);
		}
	}

	public void overrideAllPlayerPlaylist(AudioPlaylist playlist) {
		for(GuildMusicManager musicManager : musicManagers.values()) {
			musicManager.scheduler.overridePlayingPlaylist(playlist);
		}
	}
}
