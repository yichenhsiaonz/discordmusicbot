package com.discordmusicbot.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackScheduler extends AudioEventAdapter {
    private final Guild guild;
    private final AudioPlayer player;
    private final BlockingQueue<AudioTrack> queue;
    private final BlockingQueue<AudioTrack> autoPlayQueue;

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
        System.out.println("Previous artist: " + previousArtist);
        if (!player.startTrack(track, true)) {
            queue.offer(track);
        }
    }

    public void autoPlay(AudioTrack track) {
        autoPlayQueue.offer(track);
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

    public void setLoop(SlashCommandInteractionEvent event) {
        mode = event.getOption("mode").getAsString();
        switch (mode) {
            case "off":
                event.reply("Loop is off").queue();
                break;
            case "track":
                event.reply("Looping track").queue();
                break;
            case "queue":
                event.reply("Looping queue").queue();
                break;
        }
    }

    public void remove(SlashCommandInteractionEvent event) {
        int index = event.getOption("index").getAsInt();
        if(index < 1 || index > queue.size()) {
            event.reply("Invalid index").queue();
            return;
        }
        AudioTrack[] tracks = queue.toArray(new AudioTrack[0]);
        AudioTrack removed = tracks[index - 1];
        queue.remove(removed);
        event.reply("Removed track: " + removed.getInfo().title).queue();
    }

    public String skipTrack() {
        return nextTrack(player.getPlayingTrack());
    }

    public String nextTrack(AudioTrack previousTrack) {
        if(mode.equals("track")) {
            previousTrack.setPosition(0);
        } else if(mode.equals("queue")) {
            queue.offer(previousTrack.makeClone());
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
                            player.startTrack(autoPlayTrack, false);
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
        return player.getPlayingTrack().getInfo().title;
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
}
