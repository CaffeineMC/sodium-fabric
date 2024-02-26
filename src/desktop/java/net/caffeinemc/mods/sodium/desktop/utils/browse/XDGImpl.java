package net.caffeinemc.mods.sodium.desktop.utils.browse;

import net.caffeinemc.mods.sodium.desktop.utils.browse.BrowseUrlHandler;

import java.io.IOException;
import java.util.Locale;

class XDGImpl implements BrowseUrlHandler {
    public static boolean isSupported() {
        String os = System.getProperty("os.name")
                .toLowerCase(Locale.ROOT);

        return os.equals("linux");
    }

    @Override
    public void browseTo(String url) throws IOException {
        Runtime.getRuntime()
                .exec(new String[] { "xdg-open", url });
    }
}
