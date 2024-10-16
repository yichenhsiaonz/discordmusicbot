package com.discordmusicbot.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class AutoPlayResultHandler implements AudioLoadResultHandler {

  private final GuildMusicManager musicManager;

  public AutoPlayResultHandler(GuildMusicManager musicManager) {
    this.musicManager = musicManager;
  }

  @Override
  public void trackLoaded(AudioTrack track) {
    play(musicManager, track);
  }

  @Override
  public void playlistLoaded(AudioPlaylist playlist) {
    for(AudioTrack track : playlist.getTracks()) {
      play(musicManager, track);
    }
  }

  @Override
  public void noMatches() {
  }

  @Override
  public void loadFailed(FriendlyException exception) {
  }

  private void play(GuildMusicManager musicManager, AudioTrack track) {
    musicManager.scheduler.autoPlay(track);
  }
}
