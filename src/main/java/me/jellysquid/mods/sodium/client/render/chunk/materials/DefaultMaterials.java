package me.jellysquid.mods.sodium.client.render.chunk.materials;

import me.jellysquid.mods.sodium.client.render.chunk.passes.DefaultRenderPasses;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.fluid.FluidState;

public class DefaultMaterials {
    public static final Material SOLID = new Material(DefaultRenderPasses.SOLID, AlphaCutoffParameter.ZERO, true);
    public static final Material CUTOUT = new Material(DefaultRenderPasses.CUTOUT, AlphaCutoffParameter.ONE_TENTH, false);
    public static final Material CUTOUT_MIPPED = new Material(DefaultRenderPasses.CUTOUT, AlphaCutoffParameter.HALF, true);
    public static final Material TRANSLUCENT = new Material(DefaultRenderPasses.TRANSLUCENT, AlphaCutoffParameter.ZERO, true);
    public static final Material TRIPWIRE = new Material(DefaultRenderPasses.CUTOUT, AlphaCutoffParameter.ONE_TENTH, true);

    public static Material forBlockState(BlockState state) {
        return forRenderLayer(RenderLayers.getBlockLayer(state));
    }

    public static Material forFluidState(FluidState state) {
        return forRenderLayer(RenderLayers.getFluidLayer(state));
    }

    public static Material forRenderLayer(RenderLayer layer) {
        if (layer == RenderLayer.getSolid()) {
            return SOLID;
        } else if (layer == RenderLayer.getCutout()) {
            return CUTOUT;
        } else if (layer == RenderLayer.getCutoutMipped()) {
            return CUTOUT_MIPPED;
        } else if (layer == RenderLayer.getTranslucent()) {
            return TRANSLUCENT;
        } else if (layer == RenderLayer.getTripwire()) {
            return TRIPWIRE;
        }

        throw new IllegalArgumentException("No material mapping exists for " + layer);
    }
}
