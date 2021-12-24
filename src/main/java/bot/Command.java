package bot;

import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

abstract class Command extends ListenerAdapter {

    public abstract void executeCommand(@NotNull GuildVoiceJoinEvent e);

    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
        if (!event.getMember().getUser().isBot()) {
            executeCommand(event);
        }
    }

}
