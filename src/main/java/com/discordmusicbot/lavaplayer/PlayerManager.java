package com.discordmusicbot.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import static com.discordmusicbot.Main.getRefreshToken;

public class PlayerManager {
    private static PlayerManager INSTANCE = new PlayerManager();
    private final Map<Long, GuildMusicManager> musicManagers;
    private final AudioPlayerManager playerManager;

    private PlayerManager() {
        this.musicManagers = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();

        YoutubeAudioSourceManager ytSourceManager = new dev.lavalink.youtube.YoutubeAudioSourceManager(/*allowSearch:*/ true, true, false);

        String refreshToken = getRefreshToken();
        if(refreshToken == null) {
            getNewToken(ytSourceManager);
        }
        try{
            ytSourceManager.useOauth2(refreshToken, false);
        } catch (Exception e) {
            getNewToken(ytSourceManager);
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
        playerManager.loadItemOrdered(musicManager, trackUrl, new ResultHandler(event, musicManager, trackUrl));
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

    public void destroyPlayer(Guild guild) {
        musicManagers.get(Long.parseLong(guild.getId())).disconnect();
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
}
