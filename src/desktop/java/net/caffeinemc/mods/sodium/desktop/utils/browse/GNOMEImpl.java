package net.caffeinemc.mods.sodium.desktop.utils.browse;

import java.io.IOException;

class GNOMEImpl implements BrowseUrlHandler {
    public static boolean isSupported() {
        return XDGImpl.isSupported() && System.getenv("XDG_CURRENT_DESKTOP").equals("GNOME");
    }

    @Override
    public void browseTo(String url) throws IOException {
        Runtime.getRuntime()
                .exec(new String[] { "gnome-open", url });
    }
}
