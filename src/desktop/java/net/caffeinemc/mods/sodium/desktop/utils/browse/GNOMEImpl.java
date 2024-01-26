package net.caffeinemc.mods.sodium.desktop.utils.browse;

import java.io.IOException;
import java.util.Objects;

class GNOMEImpl implements BrowseUrlHandler {
    public static boolean isSupported() {
        return XDGImpl.isSupported() && Objects.equals(System.getenv("XDG_CURRENT_DESKTOP"), "GNOME");
    }

    @Override
    public void browseTo(String url) throws IOException {
        Runtime.getRuntime()
                .exec(new String[] { "gnome-open", url });
    }
}
