package net.caffeinemc.sodium.render.terrain.color.blender;

import net.caffeinemc.sodium.interop.vanilla.block.BlockColorSettings;
import net.caffeinemc.sodium.render.terrain.color.ColorSampler;
import net.caffeinemc.sodium.render.terrain.quad.ModelQuadView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.state.State;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

class ConfigurableColorBlender implements ColorBlender {
    private final ColorBlender defaultBlender;
    private final ColorBlender smoothBlender;

    public ConfigurableColorBlender(MinecraftClient client) {
        this.defaultBlender = new FlatColorBlender();
        this.smoothBlender = isSmoothBlendingEnabled(client) ? new LinearColorBlender() : this.defaultBlender;
    }

    private static boolean isSmoothBlendingEnabled(MinecraftClient client) {
        return client.options.getBiomeBlendRadius().getValue() > 0;
    }

    @Override
    public <T extends State<O, ?>, O> int[] getColors(BlockRenderView world, BlockPos origin, ModelQuadView quad, ColorSampler<T> sampler, T state) {
        ColorBlender blender;

        if (BlockColorSettings.isSmoothBlendingEnabled(world, state, origin)) {
            blender = this.smoothBlender;
        } else {
            blender = this.defaultBlender;
        }

        return blender.getColors(world, origin, quad, sampler, state);
    }
}