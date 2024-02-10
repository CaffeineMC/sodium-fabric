package net.caffeinemc.mods.sodium.client.compatibility.workarounds;

import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterInfo;
import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterProbe;
import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterVendor;
import net.minecraft.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * A utility class for keeping track of known issues and their workarounds. If a workaround is enabled, some code paths
 * may branch to follow alternative behavior, so that crashes and other issues can be avoided.
 */
public class Workarounds {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-Workarounds");

    /**
     * The currently enabled workarounds. This is stored in an atomic variable with a read-only collection type, so that
     * updating the enabled set of workarounds is guaranteed to be thread-safe.
     */
    private static final AtomicReference<Set<Reference>> ACTIVE_WORKAROUNDS = new AtomicReference<>(EnumSet.noneOf(Reference.class));

    /**
     * Scans the environment for known issues, and enables any necessary workarounds. This should only be called once
     * at early game initialization (pre-launch). If any workarounds are enabled, they will be written to the log file.
     */
    public static void init() {
        var workarounds = findNecessaryWorkarounds();

        if (!workarounds.isEmpty()) {
            LOGGER.warn("Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [{}]",
                    workarounds.stream()
                            .map(Enum::name)
                            .collect(Collectors.joining(", ")));
            LOGGER.warn("This is not necessarily an issue, but it may result in certain features or optimizations being " +
                    "disabled. You can sometimes fix these issues by upgrading your graphics driver.");
        }

        ACTIVE_WORKAROUNDS.set(workarounds);
    }

    /**
     * Scans the environment for known issues and returns the workarounds which should be enabled.
     * @return The set of workarounds which should be enabled
     */
    private static Set<Reference> findNecessaryWorkarounds() {
        var workarounds = EnumSet.noneOf(Reference.class);
        var operatingSystem = Util.getPlatform();

        var graphicsAdapters = GraphicsAdapterProbe.getAdapters();

        if (isUsingNvidiaGraphicsCard(operatingSystem, graphicsAdapters)) {
            workarounds.add(Reference.NVIDIA_THREADED_OPTIMIZATIONS);
        }

        if (isUsingWaylandSession(operatingSystem)) {
            workarounds.add(Reference.NO_ERROR_CONTEXT_UNSUPPORTED);
        }

        return Collections.unmodifiableSet(workarounds);
    }

    /**
     * @param operatingSystem The current operating system being used
     * @return True if using a Wayland desktop session on Linux, otherwise false
     */
    private static boolean isUsingWaylandSession(Util.OS operatingSystem) {
        if (operatingSystem != Util.OS.LINUX) {
            return false;
        }

        var session = System.getenv("XDG_SESSION_TYPE");

        if (session == null) {
            LOGGER.warn("Unable to determine desktop session type because the environment variable XDG_SESSION_TYPE " +
                    "is not set! Your user session may not be configured correctly.");
        }

        // BUG: This will also apply under Xwayland, even though the problem does not happen there
        return Objects.equals(session, "wayland");
    }

    /**
     * @param operatingSystem The current operating system being used
     * @param adapters The detected graphics adapters to search
     * @return True if an NVIDIA graphics adapter is detected on Windows or Linux, otherwise false
     */
    private static boolean isUsingNvidiaGraphicsCard(Util.OS operatingSystem, Collection<GraphicsAdapterInfo> adapters) {
        return (operatingSystem == Util.OS.WINDOWS || operatingSystem == Util.OS.LINUX) &&
                adapters.stream().anyMatch(adapter -> adapter.vendor() == GraphicsAdapterVendor.NVIDIA);
    }

    /**
     * @return True if the given workaround is enabled, otherwise false
     */
    public static boolean isWorkaroundEnabled(Reference id) {
        return ACTIVE_WORKAROUNDS.get()
                .contains(id);
    }

    public enum Reference {
        /**
         * The NVIDIA driver applies "Threaded Optimizations" when Minecraft is detected, causing severe
         * performance issues and crashes.
         * <a href="https://github.com/CaffeineMC/sodium-fabric/issues/1816">GitHub Issue</a>
         */
        NVIDIA_THREADED_OPTIMIZATIONS,

        /**
         * Requesting a No Error Context causes a crash at startup when using a Wayland session.
         * <a href="https://github.com/CaffeineMC/sodium-fabric/issues/1624">GitHub Issue</a>
         */
        NO_ERROR_CONTEXT_UNSUPPORTED,
    }
}
