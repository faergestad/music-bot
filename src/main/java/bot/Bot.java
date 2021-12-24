package bot;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;

@Slf4j
public class Bot {

    public static void main(String[] args) throws LoginException {
        JDABuilder.createDefault(Info.TOKEN)
                .addEventListeners(new MusicCommand())
                .setBulkDeleteSplittingEnabled(false) // Performance reasons
                .build();
    }
}
