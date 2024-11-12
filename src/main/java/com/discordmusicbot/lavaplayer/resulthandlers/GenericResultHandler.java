package com.discordmusicbot.lavaplayer.resulthandlers;

import com.discordmusicbot.lavaplayer.GuildMusicManager;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public abstract class GenericResultHandler implements AudioLoadResultHandler {

	protected GuildMusicManager musicManager = null;

	public GenericResultHandler() {
	}

	public GenericResultHandler(GuildMusicManager musicManager) {
		this.musicManager = musicManager;
	}

	@Override
	public void trackLoaded(AudioTrack track) {
		play(musicManager, track);
	}

	@Override
	public void playlistLoaded(AudioPlaylist playlist) {
		if (playlist.getName().startsWith("Search results for:")) {
			AudioTrack firstTrack = playlist.getTracks().get(0);
			play(musicManager, firstTrack);
			return;
		}

		playPlaylist(musicManager, playlist);
	}

	@Override
	public void noMatches() {
		failedToLoad();
	}

	@Override
	public void loadFailed(FriendlyException exception) {
		failedToLoad();
	}

	abstract protected void play(GuildMusicManager musicManager, AudioTrack track);

	abstract protected void playPlaylist(GuildMusicManager musicManager, AudioPlaylist playlist);

	abstract protected void failedToLoad();
}
