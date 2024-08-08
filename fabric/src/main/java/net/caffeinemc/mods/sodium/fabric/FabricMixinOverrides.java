package net.caffeinemc.mods.sodium.fabric;

import net.caffeinemc.mods.sodium.client.services.PlatformMixinOverrides;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FabricMixinOverrides implements PlatformMixinOverrides {
    protected static final String JSON_KEY_SODIUM_OPTIONS = "sodium:options";

    @Override
    public List<MixinOverride> applyModOverrides() {
        List<MixinOverride> list = new ArrayList<>();

        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            ModMetadata meta = container.getMetadata();

            if (meta.containsCustomValue(JSON_KEY_SODIUM_OPTIONS)) {
                CustomValue overrides = meta.getCustomValue(JSON_KEY_SODIUM_OPTIONS);

                if (overrides.getType() != CustomValue.CvType.OBJECT) {
                    System.out.printf("[Sodium] Mod '%s' contains invalid Sodium option overrides, ignoring", meta.getId());
                    continue;
                }

                for (Map.Entry<String, CustomValue> entry : overrides.getAsObject()) {
                    if (entry.getValue().getType() != CustomValue.CvType.BOOLEAN) {
                        System.out.printf("[Sodium] Mod '%s' attempted to override option '%s' with an invalid value, ignoring", meta.getId(), entry.getKey());
                        continue;
                    }

                    list.add(new MixinOverride(meta.getId(), entry.getKey(), entry.getValue().getAsBoolean()));
                }
            }
        }

        return list;
    }
}
