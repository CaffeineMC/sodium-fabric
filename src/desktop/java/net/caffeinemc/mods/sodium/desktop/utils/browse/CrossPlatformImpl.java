package net.caffeinemc.mods.sodium.desktop.utils.browse;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

/**
 * <p>The standard implementation which relies on OpenJDK's {@link Desktop} implementation. For Linux-based platforms,
 * this may not work correctly since it appears to rely on GNOME-specific functionality.</p>
 */
class CrossPlatformImpl implements DesktopBrowseHandler {
    public static boolean isSupported() {
        return Desktop.getDesktop()
                .isSupported(Desktop.Action.BROWSE);
    }

    @Override
    public void browseTo(URI uri) throws IOException {
        Desktop.getDesktop()
                .browse(uri);
    }
}
