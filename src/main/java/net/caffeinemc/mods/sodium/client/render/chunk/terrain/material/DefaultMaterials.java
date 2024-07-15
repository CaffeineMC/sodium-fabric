package net.caffeinemc.mods.sodium.client.render.chunk.terrain.material;

import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class DefaultMaterials {
    public static final Material SOLID = new Material(DefaultTerrainRenderPasses.SOLID, AlphaCutoffParameter.ZERO, true);
    public static final Material CUTOUT = new Material(DefaultTerrainRenderPasses.CUTOUT, AlphaCutoffParameter.ONE_TENTH, false);
    public static final Material CUTOUT_MIPPED = new Material(DefaultTerrainRenderPasses.CUTOUT, AlphaCutoffParameter.HALF, true);
    public static final Material TRANSLUCENT = new Material(DefaultTerrainRenderPasses.TRANSLUCENT, AlphaCutoffParameter.ZERO, true);
    public static final Material TRIPWIRE = new Material(DefaultTerrainRenderPasses.TRANSLUCENT, AlphaCutoffParameter.ONE_TENTH, true);

    public static Material forBlockState(BlockState state) {
        return forRenderLayer(ItemBlockRenderTypes.getChunkRenderType(state));
    }

    public static Material forFluidState(FluidState state) {
        return forRenderLayer(ItemBlockRenderTypes.getRenderLayer(state));
    }

    public static Material forRenderLayer(RenderType layer) {
        if (layer == RenderType.solid()) {
            return SOLID;
        } else if (layer == RenderType.cutout()) {
            return CUTOUT;
        } else if (layer == RenderType.cutoutMipped()) {
            return CUTOUT_MIPPED;
        } else if (layer == RenderType.tripwire()) {
            return TRIPWIRE;
        } else if (layer == RenderType.translucent()) {
            return TRANSLUCENT;
        }

        throw new IllegalArgumentException("No material mapping exists for " + layer);
    }
}
