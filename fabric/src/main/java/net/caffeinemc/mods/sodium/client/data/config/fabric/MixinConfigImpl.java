package net.caffeinemc.mods.sodium.client.data.config.fabric;

import net.caffeinemc.mods.sodium.client.data.config.MixinConfig;
import net.caffeinemc.mods.sodium.client.fabric.MixinConfigFabric;

public class MixinConfigImpl {
    public static MixinConfig create() {
        return new MixinConfigFabric();
    }
}
