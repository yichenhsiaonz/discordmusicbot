package com.discordmusicbot.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import java.util.HashMap;
import java.util.Map;

public class PlayerManager {
    private static PlayerManager INSTANCE = new PlayerManager();
    private final Map<Long, GuildMusicManager> musicManagers;
    private final AudioPlayerManager playerManager;

    private PlayerManager() {
        this.musicManagers = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();
        YoutubeAudioSourceManager ytSourceManager = new dev.lavalink.youtube.YoutubeAudioSourceManager(/*allowSearch:*/ true, true, false);
        ytSourceManager.useOauth2(null, false);


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

    public void load(){
        // prevent lazy loading
    }
}
