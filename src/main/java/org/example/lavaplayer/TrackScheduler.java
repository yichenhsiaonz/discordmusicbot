package org.example.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackScheduler extends AudioEventAdapter {
    private final AudioPlayer player;
    private final BlockingQueue<AudioTrack> queue;

    private String mode = "off";

    public TrackScheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
    }

    public void queue(AudioTrack track) {
        if (!player.startTrack(track, true)) {
            queue.offer(track);
        }
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
            skipTrack();
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
        if(mode.equals("track")) {
            player.getPlayingTrack().setPosition(0);
        } else if(mode.equals("queue")) {
            queue.offer(player.getPlayingTrack().makeClone());
            player.startTrack(queue.poll(), false);
        } else {
            AudioTrack nextTrack = queue.poll();
            if (nextTrack != null) {
                player.startTrack(nextTrack, false);
            } else {
                player.stopTrack();
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
}
