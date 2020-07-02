package me.jellysquid.mods.sodium.client;

import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.util.UnsafeUtil;
import net.fabricmc.api.ClientModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class SodiumClientMod implements ClientModInitializer {
    private static SodiumGameOptions CONFIG;
    private static Logger LOGGER;

    @Override
    public void onInitializeClient() {

    }

    public static SodiumGameOptions options() {
        if (CONFIG == null) {
            CONFIG = SodiumGameOptions.load(new File("config/sodium-options.json"));
        }

        return CONFIG;
    }

    public static Logger logger() {
        if (LOGGER == null) {
            LOGGER = LogManager.getLogger("Sodium");
        }

        return LOGGER;
    }

    public static void onConfigChanged(SodiumGameOptions options) {
        UnsafeUtil.setEnabled(options.performance.useMemoryIntrinsics);
    }
}
