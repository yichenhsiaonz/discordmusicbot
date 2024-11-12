package com.discordmusicbot.lavaplayer.resulthandlers;

import com.discordmusicbot.lavaplayer.GuildMusicManager;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class SearchResultHandler extends GenericResultHandler {

    private final SlashCommandInteractionEvent event;
    private final String trackUrl;

    public SearchResultHandler(SlashCommandInteractionEvent event, GuildMusicManager musicManager, String trackUrl) {
        super(musicManager);
        this.event = event;
        this.trackUrl = trackUrl;
        event.deferReply().queue();
    }

    protected void play(GuildMusicManager musicManager, AudioTrack track) {
        musicManager.scheduler.queue(track);
        event.getHook().sendMessage("Adding to queue top search result: " + track.getInfo().title).queue();
    }

    protected void playPlaylist(GuildMusicManager musicManager, AudioPlaylist playlist) {
        for (AudioTrack track : playlist.getTracks()) {
            musicManager.scheduler.queue(track);
        }
        event.getHook().sendMessage("Added playlist to queue: " + playlist.getName()).queue();
    }

    protected void failedToLoad() {
        event.getHook().sendMessage("Failed to load track: " + trackUrl).queue();
    }
}
