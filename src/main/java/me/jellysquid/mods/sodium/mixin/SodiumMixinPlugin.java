package me.jellysquid.mods.sodium.mixin;

import me.jellysquid.mods.sodium.common.config.Option;
import me.jellysquid.mods.sodium.common.config.SodiumConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.File;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class SodiumMixinPlugin implements IMixinConfigPlugin {
    private static final String MIXIN_PACKAGE_ROOT = "me.jellysquid.mods.sodium.mixin.";

    private final Logger logger = LogManager.getLogger("Sodium");
    private SodiumConfig config;

    @Override
    public void onLoad(String mixinPackage) {
        try {
            this.config = SodiumConfig.load(new File("./config/sodium-mixins.properties"), "/sodium.mixins.json");
        } catch (Exception e) {
            throw new RuntimeException("Could not load configuration file for Sodium", e);
        }

        this.logger.info("Loaded configuration file for Sodium ({} options available, {} user overrides)",
                this.config.getOptionCount(), this.config.getOptionOverrideCount());
        this.logger.info("Sodium has been successfully discovered and initialized -- your game is now faster!");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.startsWith(MIXIN_PACKAGE_ROOT)) {
            return true;
        }

        String mixin = mixinClassName.substring(MIXIN_PACKAGE_ROOT.length());
        Option option = this.config.getOptionForMixin(mixin);

        if (option.isUserDefined()) {
            if (option.isEnabled()) {
                this.logger.warn("Applying mixin '{}' as user configuration forcefully enables it", mixin);
            } else {
                this.logger.warn("Not applying mixin '{}' as user configuration forcefully disables it", mixin);
            }
        }

        return option.isEnabled();
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
