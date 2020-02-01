package me.jellysquid.mods.sodium.mixin;

import me.jellysquid.mods.sodium.common.SodiumMod;
import me.jellysquid.mods.sodium.common.config.SodiumConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class SodiumMixinPlugin implements IMixinConfigPlugin {
    private static final String MIXIN_PACKAGE_ROOT = "me.jellysquid.mods.sodium.mixin.";

    private final Logger logger = LogManager.getLogger("Sodium");
    private final HashSet<String> enabledPackages = new HashSet<>();

    @Override
    public void onLoad(String mixinPackage) {
        SodiumConfig config = SodiumConfig.load(new File("./config/sodium-mixins.toml"));

        this.setupMixins(config);

        this.logger.info("Sodium's configuration file was loaded successfully");

        SodiumMod.CONFIG = config;
    }

    private void setupMixins(SodiumConfig config) {
        this.enableIf("pipeline", true);
        this.enableIf("models", true);
        this.enableIf("render", true);
        this.enableIf("options", true);
        this.enableIf("fast_chunk_occlusion", true);
    }

    private void enableIf(String packageName, boolean condition) {
        if (condition) {
            this.enabledPackages.add(packageName);
        }
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

        int start = MIXIN_PACKAGE_ROOT.length();
        int lastSplit = start;
        int nextSplit;

        while ((nextSplit = mixinClassName.indexOf('.', lastSplit + 1)) != -1) {
            String part = mixinClassName.substring(start, nextSplit);

            if (this.enabledPackages.contains(part)) {
                return true;
            }

            lastSplit = nextSplit;
        }

        this.logger.info("Not applying mixin '" + mixinClassName + "' as no configuration enables it");

        return false;
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
