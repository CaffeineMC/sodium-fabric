package me.jellysquid.mods.sodium.client.util.workarounds;

import me.jellysquid.mods.sodium.client.util.workarounds.platform.NVIDIAWorkarounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DriverWorkarounds {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-DriverWorkarounds");

    public static void beforeContextCreation() {
        LOGGER.info("Checking for any workarounds that need to be applied prior to context creation...");

        if (Workarounds.isWorkaroundEnabled(Workarounds.Reference.NVIDIA_BAD_DRIVER_SETTINGS)) {
            NVIDIAWorkarounds.install();
        }

        if (Workarounds.isWorkaroundEnabled(Workarounds.Reference.NVIDIA_BAD_DRIVER_LINUX)) {
            NVIDIAWorkarounds.setLinuxDisableEnv();
        }
    }

    public static void afterContextCreation() {
        if (Workarounds.isWorkaroundEnabled(Workarounds.Reference.NVIDIA_BAD_DRIVER_SETTINGS)) {
            NVIDIAWorkarounds.uninstall();
        }
    }
}
