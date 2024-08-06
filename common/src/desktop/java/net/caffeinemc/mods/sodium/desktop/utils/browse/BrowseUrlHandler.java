package net.caffeinemc.mods.sodium.desktop.utils.browse;

import java.io.IOException;

public interface BrowseUrlHandler {
    void browseTo(String url) throws IOException;

    static BrowseUrlHandler createImplementation() {
        // OpenJDK doesn't use xdg-open and fails to provide an implementation on most systems.
        if (XDGImpl.isSupported()) {
            // Apparently xdg-open is just broken for some desktop environments, because *for some reason*
            // setting a default browser is complicated.
            if (KDEImpl.isSupported()) {
                return new KDEImpl();
            } else if (GNOMEImpl.isSupported()) {
                return new GNOMEImpl();
            }

            // If the user's desktop environment isn't KDE or GNOME, then we can only rely on xdg-open being present.
            return new XDGImpl();
        } else if (CrossPlatformImpl.isSupported()) {
            return new CrossPlatformImpl();
        }

        return null;
    }
}
