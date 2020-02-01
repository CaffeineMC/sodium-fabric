package me.jellysquid.mods.sodium.client;

import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import net.fabricmc.api.ClientModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class SodiumClientMod implements ClientModInitializer {
    private static SodiumGameOptions CONFIG;
    private static Logger LOGGER;

    @Override
    public void onInitializeClient() {
        LOGGER = LogManager.getLogger("Sodium");
        CONFIG = SodiumGameOptions.load(new File("config/sodium-options.json"));
    }

    public static SodiumGameOptions options() {
        if (CONFIG == null) {
            throw new NullPointerException("Config is not available");
        }

        return CONFIG;
    }

    public static Logger logger() {
        if (LOGGER == null) {
            throw new NullPointerException("Logger is not available");
        }

        return LOGGER;
    }
}
