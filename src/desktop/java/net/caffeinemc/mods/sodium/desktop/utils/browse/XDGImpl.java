package net.caffeinemc.mods.sodium.desktop.utils.browse;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * An implementation of {@link DesktopBrowseHandler} which uses the xdg-open utility to launch the default handler for
 * files and urls.
 *
 * <p>See also: <a href="https://manpages.ubuntu.com/manpages/noble/man1/xdg-open.1.html">xdg-open(1)</a></p>
 */
class XDGImpl implements DesktopBrowseHandler {
    // NOTE: This does not check whether xdg-open is actually present, and simply assumes that any Linux or BSD-based
    // desktop will provide an implementation of xdg-open.
    public static boolean isSupported() {
        String os = System.getProperty("os.name")
                .toLowerCase(Locale.ROOT);

        return os.equals("linux");
    }

    @Override
    public void browseTo(URI uri) throws IOException {
        Runtime.getRuntime()
                .exec(new String[] { "xdg-open", uri.toString() });
    }
}
