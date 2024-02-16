package net.caffeinemc.mods.sodium.client.fabric;

import net.caffeinemc.mods.sodium.client.data.config.MixinConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.util.Map;

public class MixinConfigFabric extends MixinConfig {
    public MixinConfigFabric() {
        super();
    }

    @Override
    public void applyModOverrides() {
        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            ModMetadata meta = container.getMetadata();

            if (meta.containsCustomValue(JSON_KEY_SODIUM_OPTIONS)) {
                CustomValue overrides = meta.getCustomValue(JSON_KEY_SODIUM_OPTIONS);

                if (overrides.getType() != CustomValue.CvType.OBJECT) {
                    LOGGER.warn("Mod '{}' contains invalid Sodium option overrides, ignoring", meta.getId());
                    continue;
                }

                for (Map.Entry<String, CustomValue> entry : overrides.getAsObject()) {
                    if (entry.getValue().getType() != CustomValue.CvType.BOOLEAN) {
                        LOGGER.warn("Mod '{}' attempted to override option '{}' with an invalid value, ignoring", meta.getId(), entry.getKey());
                        continue;
                    }

                    this.applyModOverride(meta.getId(), entry.getKey(), entry.getValue().getAsBoolean());
                }
            }
        }
    }
}
