package me.jellysquid.mods.sodium.client.util.workarounds.platform;

import me.jellysquid.mods.sodium.client.util.workarounds.platform.linux.LibC;
import me.jellysquid.mods.sodium.client.util.workarounds.platform.windows.Kernel32;
import me.jellysquid.mods.sodium.client.util.workarounds.platform.windows.WindowsProcessHacks;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NvidiaWorkarounds {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-NvidiaWorkarounds");

    public static void install() {
        LOGGER.warn("Attempting to apply workarounds for the NVIDIA Graphics Driver...");
        LOGGER.warn("If the game crashes immediately after this point, please make a bug report: https://github.com/CaffeineMC/sodium-fabric/issues");

        try {
            switch (Util.getOperatingSystem()) {
                case WINDOWS -> {
                    // The NVIDIA drivers rely on parsing the command line arguments to detect Minecraft. If we destroy those,
                    // then it shouldn't be able to detect us anymore.
                    WindowsProcessHacks.setCommandLine("net.caffeinemc.sodium");

                    // Ensures that Minecraft will run on the dedicated GPU, since the drivers can no longer detect it
                    Kernel32.setEnvironmentVariable("SHIM_MCCOMPAT", "0x800000001");
                }
                case LINUX -> {
                    // Unlike Windows, we don't need to hide ourselves from the driver. We can just request that
                    // it not use threaded optimizations instead.
                    LibC.setEnvironmentVariable("__GL_THREADED_OPTIMIZATIONS", "0");
                }
            }

            LOGGER.info("... Successfully applied workarounds for the NVIDIA Graphics Driver!");
        } catch (Throwable t) {
            LOGGER.error("Failure while applying workarounds", t);

            LOGGER.error("READ ME! The workarounds for the NVIDIA Graphics Driver did not apply correctly!");
            LOGGER.error("READ ME! You are very likely going to run into unexplained crashes and severe performance issues!");
            LOGGER.error("READ ME! Please see this issue for more information: https://github.com/CaffeineMC/sodium-fabric/issues/1816");
        }
    }

    public static void uninstall() {
        switch (Util.getOperatingSystem()) {
            case WINDOWS -> {
                WindowsProcessHacks.resetCommandLine();
            }
            case LINUX -> { }
        }
    }
}
