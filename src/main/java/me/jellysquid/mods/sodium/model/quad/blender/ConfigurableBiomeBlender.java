package me.jellysquid.mods.sodium.model.quad.blender;

import me.jellysquid.mods.sodium.interop.vanilla.block.BlockColorSettings;
import me.jellysquid.mods.sodium.model.quad.QuadColorizer;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.state.State;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

class ConfigurableBiomeBlender implements BiomeBlender {
    private final BiomeBlender defaultBlender;
    private final BiomeBlender smoothBlender;

    public ConfigurableBiomeBlender(MinecraftClient client) {
        this.defaultBlender = new VanillaBiomeBlender();
        this.smoothBlender = isSmoothBlendingEnabled(client) ? new SmoothBiomeBlender() : this.defaultBlender;
    }

    private static boolean isSmoothBlendingEnabled(MinecraftClient client) {
        return client.options.biomeBlendRadius > 0;
    }

    @Override
    public <T extends State<O, ?>, O> void getColors(BlockRenderView world, T state, BlockPos origin, QuadView quad, QuadColorizer<T> resolver, int[] colors) {
        BiomeBlender blender;

        if (BlockColorSettings.isSmoothBlendingEnabled(world, state, origin)) {
            blender = this.smoothBlender;
        } else {
            blender = this.defaultBlender;
        }

        blender.getColors(world, state, origin, quad, resolver, colors);
    }
}
