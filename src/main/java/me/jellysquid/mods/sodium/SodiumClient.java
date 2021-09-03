package me.jellysquid.mods.sodium;

import me.jellysquid.mods.sodium.config.SodiumRenderConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import me.jellysquid.mods.sodium.interop.fabric.SodiumRenderer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SodiumClient {
    private static SodiumRenderConfig CONFIG;
    private static Logger LOGGER;

    private static String MOD_VERSION;

    public static void init() {
        ModContainer mod = FabricLoader.getInstance()
                .getModContainer("sodium")
                .orElseThrow(NullPointerException::new);

        MOD_VERSION = mod.getMetadata()
                .getVersion()
                .getFriendlyString();

        RendererAccess.INSTANCE.registerRenderer(SodiumRenderer.INSTANCE);
    }

    public static SodiumRenderConfig options() {
        if (CONFIG == null) {
            CONFIG = loadConfig();
        }

        return CONFIG;
    }

    public static Logger logger() {
        if (LOGGER == null) {
            LOGGER = LogManager.getLogger("Sodium");
        }

        return LOGGER;
    }

    private static SodiumRenderConfig loadConfig() {
        return SodiumRenderConfig.load(FabricLoader.getInstance().getConfigDir().resolve("sodium-options.json"));
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
