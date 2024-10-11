package org.example;

import java.nio.charset.StandardCharsets;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.example.lavaplayer.PlayerManager;

import static net.dv8tion.jda.api.interactions.commands.OptionType.BOOLEAN;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

public class Main extends ListenerAdapter {

    private static final String KEYPATH = "discordbotkey.txt";

    public static void main(String[] args) throws Exception {
        String token = Files.readAllLines(Paths.get(KEYPATH), StandardCharsets.UTF_8).get(0);

        JDA jda = JDABuilder.createDefault(token)
            .addEventListeners(new Main())
            .build();

        CommandListUpdateAction commands = jda.updateCommands();

        commands.addCommands(
                Commands.slash("play", "Play song")
                        .addOption(STRING, "content", "Link or search", true)
        );
        commands.addCommands(
                Commands.slash("skip", "Skip song")
        );
        commands.addCommands(
                Commands.slash("queue", "Queue")
        );
        commands.addCommands(
                Commands.slash("pause", "Pause song")
        );
        commands.addCommands(
                Commands.slash("dice", "Roll a dice")
        );
        commands.addCommands(
                Commands.slash("resume", "Resume song")
        );
        commands.addCommands(
                Commands.slash("clear", "Clear queue")
        );
        commands.addCommands(
                Commands.slash("leave", "Leave voice channel")
        );
        commands.addCommands(
                Commands.slash("join", "Join voice channel")
        );
        commands.addCommands(
                Commands.slash("loop", "Loop")
                        .addOptions(
                                new OptionData(STRING, "mode", "Loop mode", true)
                                        .addChoice("Track", "track")
                                        .addChoice("Queue", "queue")
                                        .addChoice("Off", "off")
                        )
        );
        commands.addCommands(
                Commands.slash("remove", "Remove song from queue")
                        .addOption(STRING, "index", "Index of song to remove", true)
        );
        commands.addCommands(
          Commands.slash("autoplay", "Turn autoplay on or off")
                  .addOption(BOOLEAN, "boolean", "Turn autoplay on or off", true)
        );
        commands.queue();

    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String command = event.getName();

        switch (command) {
            case "play":
                joinVoiceChannel(event);
                OptionMapping option = event.getOption("content");
                String content = option.getAsString();
                PlayerManager.getInstance().loadAndPlay(event, content);
                break;
            case "skip":
                String nextTitle;
                try{
                    nextTitle = PlayerManager.getInstance().getTrackScheduler(event).skipTrack();
                } catch (Exception e) {
                    e.printStackTrace();
                    event.reply("No more tracks in queue").queue();
                    return;
                }
                event.reply("Skipped to next track: " + nextTitle).queue();
                break;
            case "queue":
                event.reply(PlayerManager.getInstance().getTrackScheduler(event).getQueue()).queue();
                break;
            case "pause":
                PlayerManager.getInstance().getTrackScheduler(event).pauseTrack();
                event.reply("Paused track").queue();
                break;
            case "resume":
                PlayerManager.getInstance().getTrackScheduler(event).resumeTrack();
                event.reply("Resumed track").queue();
                break;
            case "clear":
                PlayerManager.getInstance().getTrackScheduler(event).clearQueue();
                event.reply("Cleared queue").queue();
                break;
            case "leave":
                if (checkSameChannel(event)) {
                    event.getGuild().getAudioManager().closeAudioConnection();
                    event.reply("Left voice channel").queue();
                } else {
                    event.reply("Not in your voice channel").queue();
                }
                break;
            case "join":
                if(checkSameChannel(event)) {
                    event.reply("Already in your voice channel").queue();
                } else {
                    joinVoiceChannel(event);
                    event.reply("Joined voice channel").queue();
                }
            case "loop":
                PlayerManager.getInstance().getTrackScheduler(event).setLoop(event);
                break;
            case "remove":
                PlayerManager.getInstance().getTrackScheduler(event).remove(event);
                break;
            case "autoplay":
                boolean autoPlay = event.getOption("boolean").getAsBoolean();
                PlayerManager.getInstance().getTrackScheduler(event).setAutoPlay(autoPlay);
                event.reply("Autoplay is " + (autoPlay ? "on" : "off")).queue();
                break;
        }
    }

    private boolean checkSameChannel(SlashCommandInteractionEvent event) {
        VoiceChannel userChannel = (VoiceChannel) event.getMember().getVoiceState().getChannel();
        VoiceChannel currentChannel = (VoiceChannel) event.getGuild().getAudioManager().getConnectedChannel();
        return userChannel != null && currentChannel != null && userChannel.getId().equals(currentChannel.getId());
    }

    private void joinVoiceChannel(SlashCommandInteractionEvent event) {
        VoiceChannel userChannel = (VoiceChannel) event.getMember().getVoiceState().getChannel();
        if(userChannel != null) {
            AudioManager audioManager = event.getGuild().getAudioManager();
            audioManager.openAudioConnection(userChannel);
        } else {
            event.reply("You need to be in a voice channel").queue();
        }
    }
}
