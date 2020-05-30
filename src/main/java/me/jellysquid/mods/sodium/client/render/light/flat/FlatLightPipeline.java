package me.jellysquid.mods.sodium.client.render.light.flat;

import me.jellysquid.mods.sodium.client.render.light.LightPipeline;
import me.jellysquid.mods.sodium.client.render.light.QuadLightData;
import me.jellysquid.mods.sodium.client.render.light.cache.LightDataCache;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadFlags;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Arrays;

import static me.jellysquid.mods.sodium.client.render.light.cache.LightDataCache.unpackLM;

/**
 * A light pipeline which implements "classic-style" lighting through simply using the light value of the adjacent
 * block to a face.
 */
public class FlatLightPipeline implements LightPipeline {
    private final LightDataCache lightCache;

    public FlatLightPipeline(LightDataCache cache) {
        this.lightCache = cache;
    }

    @Override
    public void reset() {

    }

    @Override
    public void calculate(ModelQuadView quad, BlockPos pos, QuadLightData out, Direction face) {
        // No ambient occlusion exists when using flat shading.
        Arrays.fill(out.br, 1.0f);

        // If the face is aligned, use the light data above it
        if ((quad.getFlags() & ModelQuadFlags.IS_ALIGNED) != 0) {
            Arrays.fill(out.lm, unpackLM(this.lightCache.get(pos, face)));
        } else {
            Arrays.fill(out.lm, unpackLM(this.lightCache.get(pos)));
        }
    }
}
