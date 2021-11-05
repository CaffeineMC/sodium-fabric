package me.jellysquid.mods.sodium.mixin;

import net.caffeinemc.caffeineconfig.AbstractCaffeineConfigMixinPlugin;
import net.caffeinemc.caffeineconfig.CaffeineConfig;
import net.fabricmc.loader.api.FabricLoader;

public class SodiumMixinPlugin extends AbstractCaffeineConfigMixinPlugin {
    private static final String MIXIN_PACKAGE_ROOT = "me.jellysquid.mods.sodium.mixin.";

    @Override
    protected CaffeineConfig createConfig() {
        return CaffeineConfig.builder("Sodium")
                .addMixinOption("core", true) // TODO: Don't actually allow the user to disable 

                .addMixinOption("features.block", true)
                .addMixinOption("features.buffer_builder", true)
                .addMixinOption("features.buffer_builder.fast_advance", true)
                .addMixinOption("features.buffer_builder.fast_sort", true)
                .addMixinOption("features.buffer_builder.intrinsics", true)
                .addMixinOption("features.chunk_rendering", true)
                .addMixinOption("features.debug", true)
                .addMixinOption("features.entity", true)
                .addMixinOption("features.entity.fast_render", true)
                .addMixinOption("features.entity.smooth_lighting", true)
                .addMixinOption("features.gui", true)
                .addMixinOption("features.gui.fast_loading_screen", true)
                .addMixinOption("features.gui.font", true)
                .addMixinOption("features.item", true)
                .addMixinOption("features.matrix_stack", true)
                .addMixinOption("features.model", true)
                .addMixinOption("features.options", true)
                .addMixinOption("features.particle", true)
                .addMixinOption("features.particle.cull", true)
                .addMixinOption("features.particle.fast_render", true)
                .addMixinOption("features.render_layer", true)
                .addMixinOption("features.render_layer.leaves", true)
                .addMixinOption("features.sky", true)
                .addMixinOption("features.texture_tracking", true)
                .addMixinOption("features.texture_updates", true)
                .addMixinOption("features.world_ticking", true)
                .addMixinOption("features.fast_biome_colors", true)
                .addMixinOption("features.shader", true)
                .build(FabricLoader.getInstance().getConfigDir().resolve("sodium.properties"));
    }

    @Override
    protected String mixinPackageRoot() {
        return MIXIN_PACKAGE_ROOT;
    }
}
