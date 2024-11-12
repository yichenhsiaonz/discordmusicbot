package com.discordmusicbot.lavaplayer.resulthandlers;

import com.discordmusicbot.lavaplayer.GuildMusicManager;
import com.discordmusicbot.lavaplayer.PlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class OverrideResultHandler extends GenericResultHandler {

	public OverrideResultHandler() {
	}

	protected void play(GuildMusicManager musicManager, AudioTrack track) {
		PlayerManager.getInstance().overrideAllPlayers(track);
		System.out.println("Playing track: " + track.getInfo().title);
	}

	protected void playPlaylist(GuildMusicManager musicManager, AudioPlaylist playlist) {
		PlayerManager.getInstance().overrideAllPlayerPlaylist(playlist);
	}

	protected void failedToLoad() {
	}
}
