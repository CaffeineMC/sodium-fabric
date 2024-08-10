package net.caffeinemc.mods.sodium.client.model.light;

import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.FluidState;

/**
 * Light pipelines allow model quads for any location in the level to be lit using various backends, including fluids
 * and block entities. 
 */
public interface LightPipeline {
    /**
     * Calculates the light data for a given block model quad, storing the result in {@param out}.
     * @param quad The block model quad
     * @param pos The block position of the model this quad belongs to
     * @param out The data arrays which will store the calculated light data results
     * @param cullFace The cull face of the quad
     * @param lightFace The light face of the quad
     * @param shade True if the block is shaded by ambient occlusion
     * @param enhanced Whether the quad should use normal-based irregular lighting
     */
    void calculate(ModelQuadView quad, BlockPos pos, QuadLightData out, Direction cullFace, Direction lightFace, boolean shade, boolean enhanced);
}
