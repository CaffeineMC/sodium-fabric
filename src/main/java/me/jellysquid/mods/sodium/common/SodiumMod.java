package me.jellysquid.mods.sodium.common;

import me.jellysquid.mods.sodium.common.config.SodiumConfig;
import net.fabricmc.api.ModInitializer;

public class SodiumMod implements ModInitializer {
    public static SodiumConfig CONFIG;

    @Override
    public void onInitialize() {
        if (CONFIG == null) {
            throw new IllegalStateException("The mixin plugin did not initialize the config! Did it not load?");
        }
    }
}
