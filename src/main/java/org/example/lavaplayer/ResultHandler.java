package org.example.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class ResultHandler implements AudioLoadResultHandler {

    private final SlashCommandInteractionEvent event;
    private final GuildMusicManager musicManager;
    private String trackUrl;

    public ResultHandler(SlashCommandInteractionEvent event, GuildMusicManager musicManager, String trackUrl) {
        this.event = event;
        this.musicManager = musicManager;
        this.trackUrl = trackUrl;
        event.deferReply().queue();
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        event.getHook().editOriginal("Adding to queue " + track.getInfo().title).queue();
        play(musicManager, track);
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        AudioTrack firstTrack = playlist.getSelectedTrack();

        if (firstTrack == null) {
            firstTrack = playlist.getTracks().get(0);
        }

        event.getHook().editOriginal("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();

        for(AudioTrack track : playlist.getTracks()) {
            play(musicManager, track);
        }
    }

    @Override
    public void noMatches() {
        event.getHook().editOriginal("Nothing found by " + trackUrl).queue();
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        event.getHook().editOriginal("Could not play: " + exception.getMessage()).queue();
    }

    private void play(GuildMusicManager musicManager, AudioTrack track) {
        musicManager.scheduler.queue(track);
    }
}
