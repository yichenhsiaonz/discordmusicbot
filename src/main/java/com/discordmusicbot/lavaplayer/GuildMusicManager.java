package com.discordmusicbot.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.List;

public class GuildMusicManager {
    public final AudioPlayer player;
    public final TrackScheduler scheduler;
    private final Guild guild;
    private boolean active = true;
    private Thread timeout;

    public GuildMusicManager(AudioPlayerManager manager, Guild guild) {
        player = manager.createPlayer();
        scheduler = new TrackScheduler(player, guild);
        player.addListener(scheduler);
        this.guild = guild;
        timeout = new Thread(() -> {
            while (active){
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ignored) {
                }
                List<Member> members = guild.getAudioManager().getConnectedChannel().getMembers();
                int botCount = 0;
                for(Member member : members){
                    if (member.getUser().isBot()) {
                        botCount++;
                    }
                }
                if (botCount == members.size()) {
                    active = false;
                }
            }
            guild.getAudioManager().closeAudioConnection();
            PlayerManager.getInstance().deregisterPlayer(guild);
        });
        timeout.start();
    }

    public void disconnect() {
        active = false;
        timeout.interrupt();
    }

    public AudioPlayerSendHandler getSendHandler() {
        return new AudioPlayerSendHandler(player);
    }
}
