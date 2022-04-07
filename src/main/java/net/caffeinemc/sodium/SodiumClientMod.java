package net.caffeinemc.sodium;

import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.sodium.config.user.UserConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class SodiumClientMod implements ClientModInitializer {

    public static RenderDevice DEVICE;

    private static UserConfig CONFIG;
    private static Logger LOGGER;

    private static String MOD_VERSION;

    @Override
    public void onInitializeClient() {
        ModContainer mod = FabricLoader.getInstance()
                .getModContainer("sodium")
                .orElseThrow(NullPointerException::new);

        MOD_VERSION = mod.getMetadata()
                .getVersion()
                .getFriendlyString();

        LOGGER = LoggerFactory.getLogger("Sodium");

        var configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("sodium-options.json");

        loadConfig(configPath);
    }

    private static void loadConfig(Path configPath) {
        try {
            CONFIG = UserConfig.load(configPath);
        } catch (Throwable t) {
            LOGGER.error("Failed to load configuration file for Sodium", t);

            var result = TinyFileDialogs.tinyfd_messageBox("Minecraft",
                    """
                    The configuration file for Sodium could not be loaded.
                    
                    Path: %s
                    Error: %s
                    
                    The file may be corrupted or in a newer format that cannot be used by this version.
                    
                    If you continue with launching the game, the configuration file will be restored to
                    known-good defaults, and any changes you may have made will be lost.
                    
                    Additional debugging information has been saved to your game's log file.
                    """.formatted(configPath, StringUtils.truncate(t.getMessage(), 90)),
                    "okcancel",
                    "warning",
                    false);

            if (!result) {
                LOGGER.info("User asked us to not reset their config file, re-throwing error");
                throw t;
            }

            CONFIG = UserConfig.defaults(configPath);

            try {
                CONFIG.writeChanges();
            } catch (IOException e) {
                throw new RuntimeException("Failed to replace configuration file with known-good defaults", e);
            }
        }
    }

    public static UserConfig options() {
        if (CONFIG == null) {
            throw new IllegalStateException("Config not yet available");
        }

        return CONFIG;
    }

    public static Logger logger() {
        if (LOGGER == null) {
            throw new IllegalStateException("Logger not yet available");
        }

        return LOGGER;
    }

    public static String getVersion() {
        if (MOD_VERSION == null) {
            throw new NullPointerException("Mod version hasn't been populated yet");
        }

        return MOD_VERSION;
    }

    public static boolean isDirectMemoryAccessEnabled() {
        return options().advanced.allowDirectMemoryAccess;
    }
}
