package me.jellysquid.mods.sodium.client.render.chunk.terrain.material;

import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.fluid.FluidState;

public class DefaultMaterials {
    public static final Material SOLID = new Material(DefaultTerrainRenderPasses.SOLID, AlphaCutoffParameter.ZERO, true);
    public static final Material CUTOUT = new Material(DefaultTerrainRenderPasses.CUTOUT, AlphaCutoffParameter.ONE_TENTH, false);
    public static final Material CUTOUT_MIPPED = new Material(DefaultTerrainRenderPasses.CUTOUT, AlphaCutoffParameter.ONE_TENTH, true);
    public static final Material TRANSLUCENT = new Material(DefaultTerrainRenderPasses.TRANSLUCENT, AlphaCutoffParameter.ZERO, true);

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
        } else if (layer == RenderLayer.getCutoutMipped() || layer == RenderLayer.getTripwire()) {
            return CUTOUT_MIPPED;
        } else if (layer == RenderLayer.getTranslucent()) {
            return TRANSLUCENT;
        }

        throw new IllegalArgumentException("No material mapping exists for " + layer);
    }
}
