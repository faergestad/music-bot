package bot;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MusicCommand extends Command {

    private static final int PLAYLIST_LIMIT = 200;
    private static final AudioPlayerManager myManager = new DefaultAudioPlayerManager();
    private static final Map<String, Map.Entry<AudioPlayer, TrackManager>> players = new HashMap<>();

    private static final Map<String, String> userAnthemMap = Map.of(
            "185349538762981376", "https://www.youtube.com/watch?v=Xx8TQfhRVXY",
            "149247103279955971", "https://www.youtube.com/watch?v=4X4YcTMn9YQ",
            "323960367220785152", "https://youtu.be/1uvr7CJazqE?t=2");

    public MusicCommand() {
        AudioSourceManagers.registerRemoteSources(myManager);
    }

    @Override
    public void executeCommand(@NotNull GuildVoiceJoinEvent e) {
        var userAnthem = getUserAnthem(e);
        if (userAnthem != null) {
            loadTrack(userAnthem, e.getMember());
        }
    }

    private String getUserAnthem(GuildVoiceJoinEvent event) {
        return userAnthemMap.get(event.getMember().getUser().getId());
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if (!players.containsKey(event.getGuild().getId()))
            return; //Guild doesn't have a music player

        TrackManager manager = getTrackManager(event.getGuild());
        manager.getQueuedTracks().stream()
                .filter(info -> !info.getTrack().equals(getPlayer(event.getGuild()).getPlayingTrack())
                        && info.getAuthor().getUser().equals(event.getMember().getUser()))
                .forEach(manager::remove);
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        reset(event.getGuild());
    }

    private boolean hasPlayer(Guild guild) {
        return players.containsKey(guild.getId());
    }

    private AudioPlayer getPlayer(Guild guild) {
        AudioPlayer p;
        if (hasPlayer(guild)) {
            p = players.get(guild.getId()).getKey();
        } else {
            p = createPlayer(guild);
        }
        return p;
    }

    private TrackManager getTrackManager(Guild guild) {
        return players.get(guild.getId()).getValue();
    }

    private AudioPlayer createPlayer(Guild guild) {
        AudioPlayer nPlayer = myManager.createPlayer();
        TrackManager manager = new TrackManager(nPlayer);
        nPlayer.addListener(manager);
        guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(nPlayer));
        players.put(guild.getId(), new AbstractMap.SimpleEntry<>(nPlayer, manager));
        return nPlayer;
    }

    private void reset(Guild guild) {
        players.remove(guild.getId());
        getPlayer(guild).destroy();
        getTrackManager(guild).purgeQueue();
        guild.getAudioManager().closeAudioConnection();
    }

    private void loadTrack(String identifier, Member author) {
        Guild guild = author.getGuild();
        getPlayer(guild); // Make sure this guild has a player.

        myManager.loadItemOrdered(guild, identifier, new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack track) {
                getTrackManager(guild).queue(track, author);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.getSelectedTrack() != null) {
                    trackLoaded(playlist.getSelectedTrack());
                } else if (playlist.isSearchResult()) {
                    trackLoaded(playlist.getTracks().get(0));
                } else {
                    for (int i = 0; i < Math.min(playlist.getTracks().size(), PLAYLIST_LIMIT); i++) {
                        getTrackManager(guild).queue(playlist.getTracks().get(i), author);
                    }
                }
            }

            @Override
            public void noMatches() {
            }

            @Override
            public void loadFailed(FriendlyException exception) {
            }
        });
    }

}
