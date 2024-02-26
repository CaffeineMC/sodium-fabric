package net.caffeinemc.mods.sodium.client.data.config.neoforge;

import com.sun.jna.platform.unix.LibC;
import net.caffeinemc.mods.sodium.client.data.config.MixinConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import org.lwjgl.system.Configuration;

import java.util.Map;

public class MixinConfigImpl extends MixinConfig {
    public MixinConfigImpl() {
        super();
    }

    public static MixinConfig create() {
        LibC.INSTANCE.setenv("__GL_THREADED_OPTIMIZATIONS", "0", 1);
        Configuration.GLFW_LIBRARY_NAME.set("/usr/lib/libglfw.so");
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
