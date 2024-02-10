package net.caffeinemc.mods.sodium.client.compatibility.checks;

import com.mojang.blaze3d.platform.Window;
import net.caffeinemc.mods.sodium.client.platform.MessageBox;
import net.caffeinemc.mods.sodium.client.platform.windows.api.Kernel32;
import net.caffeinemc.mods.sodium.client.platform.windows.api.version.Version;
import net.minecraft.client.Minecraft;
import net.minecraft.util.NativeModuleLister;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
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

    /**
     * The known names of the injected module for RivaTuner Statistics Server.
     */
    private static final String[] RTSS_HOOKS_MODULE_NAMES = { "RTSSHooks64.dll", "RTSSHooks.dll" };

    /**
     * Scans the process for loaded modules, and checks whether any of them are known to be incompatible. If there are
     * incompatible modules, this function will abort the application with an error.
     */
    public static void checkModules() {
        List<NativeModuleLister.NativeModuleInfo> modules;

        try {
            modules = NativeModuleLister.listModules();
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

    /**
     * Checks if the currently loaded module for RivaTuner Statistics Server is known to be incompatible, and if so,
     * aborts the process with an error. This function must only be called if the module was found to be injected,
     * since if it fails to identify the version of the module (such as when it doesn't exist), it will always abort.
     */
    private static void checkRTSSModules() {
        LOGGER.warn("RivaTuner Statistics Server (RTSS) has injected into the process! Attempting to apply workarounds for compatibility...");

        String version = null;

        try {
            version = findRTSSModuleVersion();
        } catch (Throwable t) {
            LOGGER.error("Unhandled exception while parsing module version information", t);
        }

        if (version == null) {
            LOGGER.error("Could not determine version of RivaTuner Statistics Server!");
        } else {
            LOGGER.info("Detected RivaTuner Statistics Server version: {}", version);
        }

        if (!isRTSSCompatible(version)) {
            Window window = Minecraft.getInstance().getWindow();
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

    /**
     * Checks if the version string of RivaTuner Statistics Server matches a known compatible version of the software.
     * @param version The version string obtained from the PE module information of RTSS.exe
     * @return True if the version is known to be compatible, otherwise false
     */
    private static boolean isRTSSCompatible(@Nullable String version) {
        // If the version couldn't be queried, assume it is incompatible
        if (version == null) {
            return false;
        }

        var matcher = RTSS_VERSION_PATTERN.matcher(version);

        // If the version string does not match a known format, assume it is incompatible
        if (!matcher.matches()) {
            return false;
        }

        try {
            int x = Integer.parseInt(matcher.group("x"));
            int y = Integer.parseInt(matcher.group("y"));
            int z = Integer.parseInt(matcher.group("z"));

            // >=7.3.4 is known to be compatible
            return x > 7 || (x == 7 && y > 3) || (x == 7 && y == 3 && z >= 4);
        } catch (NumberFormatException ignored) {
            LOGGER.warn("Invalid version string: {}", version);
        }

        // The version string could not be parsed, assume it is incompatible
        return false;
    }

    /**
     * <p>Attempts to locate the installation directory of RivaTuner Statistics Server by scanning the directory tree
     * upwards from where RTSSHooks.dll was loaded, and returns the version of the RTSS.exe file.</p>
     *
     * <p>This is necessary because the RTSSHooks.dll file itself does not contain any version information (for unknown
     * reasons.) We also don't want to hard-code installation paths since it's not guaranteed to be there.</p>
     *
     * @return The version string from the PE module information of RTSS.exe, or null if it couldn't be found
     */
    private static @Nullable String findRTSSModuleVersion() {
        long module;

        try {
            module = Kernel32.getModuleHandleByName(RTSS_HOOKS_MODULE_NAMES);

            if (module == MemoryUtil.NULL) {
                LOGGER.warn("Could not find any modules in the process with a matching name");
                return null;
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to query the process for modules by name", t);
            return null;
        }

        String moduleFileName;

        try {
            moduleFileName = Kernel32.getModuleFileName(module);
        } catch (Throwable t) {
            LOGGER.warn("Failed to obtain the path of the module", t);
            return null;
        }

        var modulePath = Path.of(moduleFileName);
        var moduleDirectory = modulePath.getParent();

        LOGGER.info("Searching directory: {}", moduleDirectory);

        var executablePath = moduleDirectory.resolve("RTSS.exe");

        if (!Files.exists(executablePath)) {
            LOGGER.warn("Could not find RTSS.exe within the directory: {}", executablePath);
            return null;
        }

        LOGGER.info("Parsing file: {}", executablePath);

        var version = Version.getModuleFileVersion(executablePath.toAbsolutePath().toString());

        if (version == null) {
            LOGGER.warn("Could not determine the version from the PE module");
            return null;
        }

        var translation = version.queryEnglishTranslation();

        if (translation == null) {
            LOGGER.warn("Could not find a suitable code page for the PE module");
            return null;
        }

        var fileVersion = version.queryValue("FileVersion", translation);

        if (fileVersion == null) {
            LOGGER.warn("Could not find the FileVersion entry in the PE module");
            return null;
        }

        return fileVersion;
    }

    /**
     * Checks the provided list of native modules to see if any modules match the given names. The names are compared
     * in case-insensitive fashion.
     *
     * @param modules The list of native modules
     * @param names The module names to scan for
     * @return True if {@param modules} contains a name specified by {@param names}, otherwise false
     */
    private static boolean isModuleLoaded(List<NativeModuleLister.NativeModuleInfo> modules, String[] names) {
        for (var name : names) {
            for (var module : modules) {
                if (module.name.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }

        return false;
    }
}
