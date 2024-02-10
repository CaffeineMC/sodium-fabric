package net.caffeinemc.mods.sodium.desktop.utils.browse;

import java.io.IOException;
import java.net.URI;

public interface DesktopBrowseHandler {
    /**
     * Launches the user's default web browser to display the {@link URI}. If the default browser is not registered,
     * the default application registered for the URI scheme will be used instead.
     *
     * @param uri The URI to be displayed in the default application
     * @throws IOException If the default application could not be found, or was unable to be launched
     */
    void browseTo(URI uri) throws IOException;

    /**
     * Creates an implementation of {@link DesktopBrowseHandler} for the current platform.
     * @return A new instance of an implementation, or null if none is available
     */
    static DesktopBrowseHandler createImplementation() {
        // OpenJDK doesn't use xdg-open and fails to provide an implementation on most Linux systems.
        // So, if we can detect we're on a Linux-based platform, try to use xdg-open for browsing to URLs.
        if (XDGImpl.isSupported()) {
            return new XDGImpl();
        } else if (CrossPlatformImpl.isSupported()) {
            return new CrossPlatformImpl();
        }

        return null;
    }
}
