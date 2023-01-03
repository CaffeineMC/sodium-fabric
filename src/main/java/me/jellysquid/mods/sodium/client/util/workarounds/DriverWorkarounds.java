package me.jellysquid.mods.sodium.client.util.workarounds;

import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class DriverWorkarounds {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium");

    private static final AtomicReference<Set<Reference>> ACTIVE_WORKAROUNDS = new AtomicReference<>(EnumSet.noneOf(Reference.class));

    public static void init() {
        LOGGER.info("Launching a new process to probe the system configuration!");

        DriverProbeResult result;

        try {
            result = DriverProbeLauncher.launchProbe();
        } catch (Throwable t) {
            LOGGER.error("Failed to launch driver probe", t);
            return;
        }

        LOGGER.info("OpenGL Vendor: {}", result.vendor);
        LOGGER.info("OpenGL Renderer: {}", result.renderer);
        LOGGER.info("OpenGL Version: {}", result.version);

        var workarounds = updateWorkarounds(result);

        if (!workarounds.isEmpty()) {
            LOGGER.warn("One or more workarounds were enabled to prevent crashes or other issues on your system. You may need to update your graphics drivers.");
        }

        ACTIVE_WORKAROUNDS.set(workarounds);
    }

    private static Set<Reference> updateWorkarounds(DriverProbeResult probe) {
        var workarounds = EnumSet.noneOf(Reference.class);
        var operatingSystem = Util.getOperatingSystem();

        if (operatingSystem == Util.OperatingSystem.WINDOWS && probe.vendor.contains("NVIDIA")) {
            workarounds.add(Reference.ISSUE_1486);
            LOGGER.warn("Enabling workaround for NVIDIA graphics drivers on Windows (issue #1486)");
        }

        return workarounds;
    }

    public static boolean isWorkaroundEnabled(Reference id) {
        return ACTIVE_WORKAROUNDS.get().contains(id);
    }

    public enum Reference {
        ISSUE_1486
    }
}
