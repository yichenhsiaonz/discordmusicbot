package com.discordmusicbot.lavaplayer.resulthandlers;

import com.discordmusicbot.lavaplayer.GuildMusicManager;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class AutoPlayResultHandler extends GenericResultHandler {

	public AutoPlayResultHandler(GuildMusicManager musicManager) {
		super(musicManager);
	}

	@Override
	public void playlistLoaded(AudioPlaylist playlist) {
		playPlaylist(musicManager, playlist);
	}

	@Override
	protected void play(GuildMusicManager musicManager, AudioTrack track) {
		System.out.println("Playing track: " + track.getInfo().title);
		musicManager.scheduler.autoPlay(track);
	}

	@Override
	protected void playPlaylist(GuildMusicManager musicManager, AudioPlaylist playlist) {
		System.out.println("Playing playlist: " + playlist.getName());
		for (AudioTrack track : playlist.getTracks()) {
			musicManager.scheduler.autoPlay(track);
		}
	}

	@Override
	protected void failedToLoad() {
		// Do nothing
	}
}
