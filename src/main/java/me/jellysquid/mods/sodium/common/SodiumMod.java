package me.jellysquid.mods.sodium.common;

import me.jellysquid.mods.sodium.common.config.SodiumConfig;
import net.fabricmc.api.ModInitializer;

public class SodiumMod implements ModInitializer {
    public static SodiumConfig CONFIG;

    @Override
    public void onInitialize() {
        String javaVersion = System.getProperty("java.specification.version");
        String[] parsed = javaVersion.split("\\.");
        if (Integer.parseInt(parsed[parsed.length > 1 ? 1 : 0]) < 9) {
            throw new IllegalStateException("Sodium requires Java 9 or newer!");
        }

        if (CONFIG == null) {
            throw new IllegalStateException("The mixin plugin did not initialize the config! Did it not load?");
        }
    }
}
