package me.jellysquid.mods.sodium.client.util.workarounds;

import me.jellysquid.mods.sodium.client.gui.console.Console;
import me.jellysquid.mods.sodium.client.gui.console.message.MessageLevel;
import me.jellysquid.mods.sodium.client.util.workarounds.driver.nvidia.NvidiaGLContextInfo;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostLaunchChecks {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-PostlaunchChecks");

    public static void checkContext() {
        checkContextImplementation();

        if (isUsingPojavLauncher()) {
            showConsoleMessage(Text.translatable("sodium.console.pojav_launcher"));
            logMessage("It appears that PojavLauncher is being used with an OpenGL compatibility layer. This will " +
                    "likely cause severe performance issues, graphical issues, and crashes when used with Sodium. This " +
                    "configuration is not supported -- you are on your own!");
        }
    }

    private static void checkContextImplementation() {
        GLContextInfo driver = getGraphicsContextInfo();

        if (driver == null) {
            LOGGER.warn("Could not retrieve identifying strings for OpenGL implementation");
            return;
        }

        LOGGER.info("OpenGL Vendor: {}", driver.vendor());
        LOGGER.info("OpenGL Renderer: {}", driver.renderer());
        LOGGER.info("OpenGL Version: {}", driver.version());

        if (isBrokenNvidiaDriverInstalled(driver)) {
            showConsoleMessage(Text.translatable("sodium.console.broken_nvidia_driver"));
            logMessage("The NVIDIA graphics driver appears to be out of date. This will likely cause severe " +
                    "performance issues and crashes when used with Sodium. The graphics driver should be updated to " +
                    "the latest version (version 536.23 or newer).");
        }
    }

    @Nullable
    private static GLContextInfo getGraphicsContextInfo() {
        String vendor = GL11C.glGetString(GL11C.GL_VENDOR);
        String renderer = GL11C.glGetString(GL11C.GL_RENDERER);
        String version = GL11C.glGetString(GL11C.GL_VERSION);

        if (vendor == null || renderer == null || version == null) {
            return null;
        }

        return new GLContextInfo(vendor, renderer, version);
    }

    private static void showConsoleMessage(MutableText message) {
        Console.instance().logMessage(MessageLevel.SEVERE, message, 30.0);
    }

    private static void logMessage(String message, Object... args) {
        LOGGER.error(message, args);
    }

    // https://github.com/CaffeineMC/sodium-fabric/issues/1486
    // The way which NVIDIA tries to detect the Minecraft process could not be circumvented until fairly recently
    // So we require that an up-to-date graphics driver is installed so that our workarounds can disable the Threaded
    // Optimizations driver hack.
    private static boolean isBrokenNvidiaDriverInstalled(GLContextInfo driver) {
        // The Linux driver has two separate branches which have overlapping version numbers, despite also having
        // different feature sets. As a result, we can't reliably determine which Linux drivers are broken...
        if (Util.getOperatingSystem() != Util.OperatingSystem.WINDOWS) {
            return false;
        }

        var version = NvidiaGLContextInfo.tryParse(driver);

        if (version != null) {
            return version.isWithinRange(
                    new NvidiaGLContextInfo(526, 47), // Broken in 526.47
                    new NvidiaGLContextInfo(536, 23) // Fixed in 536.23
            );
        }

        return false;
    }

    // https://github.com/CaffeineMC/sodium-fabric/issues/1916
    private static boolean isUsingPojavLauncher() {
        if (System.getenv("POJAV_RENDERER") != null) {
            LOGGER.warn("Detected presence of environment variable POJAV_LAUNCHER, which seems to indicate we are running on Android");

            return true;
        }

        var librarySearchPaths = System.getProperty("java.library.path", null);

        if (librarySearchPaths != null) {
            for (var path : librarySearchPaths.split(":")) {
                if (isKnownAndroidPathFragment(path)) {
                    LOGGER.warn("Found a library search path which seems to be hosted in an Android filesystem: {}", path);

                    return true;
                }
            }
        }

        var workingDirectory = System.getProperty("user.home", null);

        if (workingDirectory != null) {
            if (isKnownAndroidPathFragment(workingDirectory)) {
                LOGGER.warn("Working directory seems to be hosted in an Android filesystem: {}", workingDirectory);
            }
        }

        return false;
    }

    private static boolean isKnownAndroidPathFragment(String path) {
        return path.matches("/data/user/[0-9]+/net\\.kdt\\.pojavlaunch");
    }

}
