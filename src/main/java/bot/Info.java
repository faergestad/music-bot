package bot;

public class Info {
    private static final Config CONFIG = new Config();
    static final String TOKEN = CONFIG.getValue("token");
}
