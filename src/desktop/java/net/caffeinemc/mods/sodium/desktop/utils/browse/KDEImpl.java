package net.caffeinemc.mods.sodium.desktop.utils.browse;

import java.io.IOException;

class KDEImpl implements BrowseUrlHandler {
    public static boolean isSupported() {
        return XDGImpl.isSupported() && System.getenv("XDG_CURRENT_DESKTOP").equals("KDE");
    }

    @Override
    public void browseTo(String url) throws IOException {
        Runtime.getRuntime()
                .exec(new String[] { "kde-open", url });
    }
}
