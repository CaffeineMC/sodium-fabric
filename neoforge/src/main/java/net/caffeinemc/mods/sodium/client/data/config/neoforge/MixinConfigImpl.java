package net.caffeinemc.mods.sodium.client.data.config.neoforge;

import net.caffeinemc.mods.sodium.client.data.config.MixinConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModInfo;

import java.util.Map;

public class MixinConfigImpl extends MixinConfig {
    public MixinConfigImpl() {
        super();
    }

    public static MixinConfig create() {
        return new MixinConfigImpl();
    }

    @Override
    public void applyModOverrides() {
        for (ModInfo meta : FMLLoader.getLoadingModList().getMods()) {
            meta.getConfigElement(JSON_KEY_SODIUM_OPTIONS).ifPresent(override -> {
                if (override instanceof Map<?, ?> overrides && overrides.keySet().stream().allMatch(key -> key instanceof String)) {
                    overrides.forEach((key, value) -> {
                        if (!(value instanceof Boolean) || !(key instanceof String)) {
                            LOGGER.warn("Mod '{}' attempted to override option '{}' with an invalid value, ignoring", meta.getModId(), key);
                            return;
                        }

                        this.applyModOverride(meta.getModId(), (String) key, (Boolean) value);
                    });
                } else {
                    LOGGER.warn("Mod '{}' contains invalid Sodium option overrides, ignoring", meta.getModId());
                }
            });
        }
    }
}
