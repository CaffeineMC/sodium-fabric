package me.jellysquid.mods.sodium.client.compatibility.checks;

import me.jellysquid.mods.sodium.client.platform.MessageBox;
import me.jellysquid.mods.sodium.client.platform.windows.api.Kernel32;
import me.jellysquid.mods.sodium.client.platform.windows.api.version.Version;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.util.WinNativeModuleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class for determining whether the current process has been injected into or otherwise modified. This should
 * generally only be accessed after OpenGL context creation, as most third-party software waits until the OpenGL ICD
 * is initialized before injecting.
 */
public class ModuleScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-Win32ModuleChecks");

    private static final String[] RTSS_HOOKS_MODULE_NAMES = { "RTSSHooks64.dll", "RTSSHooks.dll" };

    public static void checkModules() {
        List<WinNativeModuleUtil.NativeModule> modules;

        try {
            modules = WinNativeModuleUtil.collectNativeModules();
        } catch (Throwable t) {
            LOGGER.warn("Failed to scan the currently loaded modules", t);
            return;
        }

        if (modules.isEmpty()) {
            // Platforms other than Windows will not return anything.
            return;
        }

        // RivaTuner hooks the wglCreateContext function, and leaves itself behind as a loaded module
        if (Configuration.WIN32_RTSS_HOOKS && isModuleLoaded(modules, RTSS_HOOKS_MODULE_NAMES)) {
            checkRTSSModules();
        }
    }

    private static void checkRTSSModules() {
        LOGGER.warn("RivaTuner Statistics Server (RTSS) has injected into the process! Attempting to apply workarounds for compatibility...");

        String version = null;

        try {
            version = findRTSSModuleVersion();
        } catch (Throwable t) {
            LOGGER.warn("Exception thrown while reading file version", t);
        }

        if (version == null) {
            LOGGER.warn("Could not determine version of RivaTuner Statistics Server");
        } else {
            LOGGER.info("Detected RivaTuner Statistics Server version: {}", version);
        }

        if (version == null || !isRTSSCompatible(version)) {
            Window window = MinecraftClient.getInstance().getWindow();
            MessageBox.showMessageBox(window, MessageBox.IconType.ERROR, "Sodium Renderer",
                    "You appear to be using an older version of RivaTuner Statistics Server (RTSS) which is not compatible with Sodium. " +
                            "You must either update to a newer version (7.3.4 and later) or close the RivaTuner Statistics Server application.\n\n" +
                            "For more information on how to solve this problem, click the 'Help' button.",
                    "https://github.com/CaffeineMC/sodium-fabric/wiki/Known-Issues#rtss-incompatible");
            
            throw new RuntimeException("RivaTuner Statistics Server (RTSS) is not compatible with Sodium, " +
                    "see here for more details: https://github.com/CaffeineMC/sodium-fabric/wiki/Known-Issues#rtss-incompatible");
        }
    }

    // BUG: For some reason, the version string can either be in the format of "X.Y.Z.W" or "X, Y, Z, W"...
    // This does not make sense, and probably points to our handling of code pages being wrong. But for the time being,
    // the regex has been made to handle both formats, because looking at Win32 code any longer is going to break me.
    private static final Pattern RTSS_VERSION_PATTERN = Pattern.compile("^(?<x>\\d*)(, |\\.)(?<y>\\d*)(, |\\.)(?<z>\\d*)(, |\\.)(?<w>\\d*)$");

    private static boolean isRTSSCompatible(String version) {
        var matcher = RTSS_VERSION_PATTERN.matcher(version);

        if (!matcher.matches()) {
            return false;
        }

        try {
            int x = Integer.parseInt(matcher.group("x"));
            int y = Integer.parseInt(matcher.group("y"));
            int z = Integer.parseInt(matcher.group("z"));

            // >=7.3.4
            return x > 7 || (x == 7 && y > 3) || (x == 7 && y == 3 && z >= 4);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid version string: {}", version);
        }

        return false;
    }

    private static String findRTSSModuleVersion() {
        long module;

        try {
            module = Kernel32.getModuleHandleByNames(RTSS_HOOKS_MODULE_NAMES);
        } catch (Throwable t) {
            LOGGER.warn("Failed to locate module", t);
            return null;
        }

        String moduleFileName;

        try {
            moduleFileName = Kernel32.getModuleFileName(module);
        } catch (Throwable t) {
            LOGGER.warn("Failed to get path of module", t);
            return null;
        }

        var modulePath = Path.of(moduleFileName);
        var moduleDirectory = modulePath.getParent();

        LOGGER.info("Searching directory: {}", moduleDirectory);

        var executablePath = moduleDirectory.resolve("RTSS.exe");

        if (!Files.exists(executablePath)) {
            LOGGER.warn("Could not find executable: {}", executablePath);
            return null;
        }

        LOGGER.info("Parsing file: {}", executablePath);

        var version = Version.getModuleFileVersion(executablePath.toAbsolutePath().toString());

        if (version == null) {
            LOGGER.warn("Couldn't find version structure");
            return null;
        }

        var translation = version.queryEnglishTranslation();

        if (translation == null) {
            LOGGER.warn("Couldn't find suitable translation");
            return null;
        }

        var fileVersion = version.queryValue("FileVersion", translation);

        if (fileVersion == null) {
            LOGGER.warn("Couldn't query file version");
            return null;
        }

        return fileVersion;
    }

    private static boolean isModuleLoaded(List<WinNativeModuleUtil.NativeModule> modules, String[] names) {
        for (var name : names) {
            for (var module : modules) {
                if (module.path.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }

        return false;
    }
}
