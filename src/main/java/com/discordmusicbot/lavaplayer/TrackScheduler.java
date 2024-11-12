package com.discordmusicbot.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.Guild;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackScheduler extends AudioEventAdapter {
	private final Guild guild;
	private final AudioPlayer player;
	private final BlockingQueue<AudioTrack> autoPlayQueue;

	private BlockingQueue<AudioTrack> queue;
	private String mode = "off";
	private boolean autoPlay = false;
	private String previousArtist = "";

	public TrackScheduler(AudioPlayer player, Guild guild) {
		this.player = player;
		this.queue = new LinkedBlockingQueue<>();
		this.autoPlayQueue = new LinkedBlockingQueue<>();
		this.guild = guild;
	}

	public void queue(AudioTrack track) {
		previousArtist = track.getInfo().author;
		autoPlayQueue.clear();
		if (!player.startTrack(track, true)) {
			queue.add(track);
		}
	}

	public void autoPlay(AudioTrack track) {
		autoPlayQueue.add(track);
	}

	public String getQueue() {
		StringBuilder sb = new StringBuilder();
		sb.append("Current queue:\n");
		int i = 1;
		for (AudioTrack track : queue) {
			sb.append(i++).append(". ");
			sb.append(track.getInfo().title).append("\n");
		}
		return sb.toString();
	}

	public void clearQueue() {
		queue.clear();
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		if (endReason.mayStartNext) {
			nextTrack(track);
		}
	}

	public String setLoop(String mode) {
		if(mode.equals("track") || mode.equals("queue") || mode.equals("off")) {
			this.mode = mode;
			return "Loop mode set to: " + mode;
		} else {
			return "Invalid loop mode";
		}
	}

	public String remove(Integer index) {
		if(index < 1 || index > queue.size()) {
			return "Invalid index";
		}
		AudioTrack[] tracks = queue.toArray(new AudioTrack[0]);
		AudioTrack removed = tracks[index - 1];
		queue.remove(removed);
		return "Removed track: " + removed.getInfo().title;
	}

	public String skipTrack() {
		return nextTrack(player.getPlayingTrack());
	}

	public String nextTrack(AudioTrack previousTrack) {
		if(mode.equals("track")) {
			previousTrack.setPosition(0);
		} else if(mode.equals("queue")) {
			queue.add(previousTrack.makeClone());
			player.startTrack(queue.poll(), false);
		} else {
			AudioTrack nextTrack = queue.poll();
			if (nextTrack != null) {
				player.startTrack(nextTrack, false);
			} else {
				player.stopTrack();
				if(!previousArtist.isEmpty() && autoPlay) {
					AudioTrack autoPlayTrack = autoPlayQueue.poll();
					if(autoPlayTrack == null) {
						PlayerManager.getInstance().autoPlay(guild, previousArtist);
						try {
							autoPlayTrack = autoPlayQueue.take();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

					if(previousTrack.getInfo().title.equals(autoPlayTrack.getInfo().title)) {
						try{
							autoPlayTrack = autoPlayQueue.take();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					player.startTrack(autoPlayTrack, false);
				} else {
					autoPlayQueue.clear();
				}
			}
		}
		try{
			return player.getPlayingTrack().getInfo().title;
		} catch (NullPointerException e) {
			return "No more tracks in queue";
		}
	}

	public void pauseTrack() {
		player.setPaused(true);
	}

	public void resumeTrack() {
		player.setPaused(false);
	}

	public void setAutoPlay(boolean autoPlay) {
		this.autoPlay = autoPlay;
	}

	public void overridePlayingTrack(AudioTrack track) {
		List<AudioTrack> tracks = new ArrayList<>();
		tracks.add(track.makeClone());
		try{
			AudioTrack clone = duplicateCurrentTrackWithPosition();
			tracks.add(clone);
		} catch (NullPointerException e) {
			// Do nothing
		}
		tracks.addAll(queue);
		queue = new LinkedBlockingQueue<>(tracks);
		skipTrack();
	}

	public void overridePlayingPlaylist(AudioPlaylist playlist) {
		List<AudioTrack> tracks = new ArrayList<>();
		for(AudioTrack track : playlist.getTracks()) {
			tracks.add(track.makeClone());
		}
		try{
			AudioTrack clone = duplicateCurrentTrackWithPosition();
			tracks.add(clone);
		} catch (NullPointerException e) {
			// Do nothing
		}
		tracks.addAll(queue);
		queue = new LinkedBlockingQueue<>(tracks);
		skipTrack();
	}

	private AudioTrack duplicateCurrentTrackWithPosition() throws NullPointerException {
		AudioTrack track = player.getPlayingTrack();
		AudioTrack clone = track.makeClone();
		clone.setPosition(track.getPosition());
		return clone;
	}
}
