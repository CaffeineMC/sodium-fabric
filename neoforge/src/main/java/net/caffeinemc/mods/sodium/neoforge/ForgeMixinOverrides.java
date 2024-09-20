package net.caffeinemc.mods.sodium.neoforge;

import net.caffeinemc.mods.sodium.client.services.PlatformMixinOverrides;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ForgeMixinOverrides implements PlatformMixinOverrides {
    protected static final String JSON_KEY_SODIUM_OPTIONS = "sodium:options";

    public ForgeMixinOverrides() {
        super();
    }

    @Override
    public List<MixinOverride> applyModOverrides() {
        List<MixinOverride> list = new ArrayList<>();

        for (ModInfo meta : FMLLoader.getLoadingModList().getMods()) {
            meta.getConfigElement(JSON_KEY_SODIUM_OPTIONS).ifPresent(override -> {
                if (override instanceof Map<?, ?> overrides && overrides.keySet().stream().allMatch(key -> key instanceof String)) {
                    overrides.forEach((key, value) -> {
                        if (!(value instanceof Boolean) || !(key instanceof String)) {
                            System.out.printf("[Sodium] Mod '%s' attempted to override option '%s' with an invalid value, ignoring", meta.getModId(), key);
                            return;
                        }

                        list.add(new MixinOverride(meta.getModId(), (String) key, (Boolean) value));
                    });
                } else {
                    System.out.printf("[Sodium] '%s' contains invalid Sodium option overrides, ignoring", meta.getModId());
                }
            });
        }

        return list;
    }
}
