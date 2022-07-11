package net.caffeinemc.sodium.render.terrain.light.flat;

import net.caffeinemc.sodium.render.terrain.light.LightPipeline;
import net.caffeinemc.sodium.render.terrain.light.data.LightDataAccess;
import net.caffeinemc.sodium.render.terrain.light.data.QuadLightData;
import net.caffeinemc.sodium.render.terrain.quad.ModelQuadView;
import net.caffeinemc.sodium.render.terrain.quad.properties.ModelQuadFlags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Arrays;

/**
 * A light pipeline which implements "classic-style" lighting through simply using the light value of the adjacent
 * block to a face.
 */
public class FlatLightPipeline implements LightPipeline {
    /**
     * The cache which light data will be accessed from.
     */
    private final LightDataAccess lightCache;

    public FlatLightPipeline(LightDataAccess lightCache) {
        this.lightCache = lightCache;
    }

    @Override
    public void calculate(ModelQuadView quad, BlockPos pos, QuadLightData out, Direction cullFace, Direction face, boolean shade) {
        int lightmap;

        // To match vanilla behavior, use the cull face if it exists/is available
        if (cullFace != null) {
            lightmap = LightDataAccess.unpackLM(this.lightCache.get(pos, cullFace));
        } else {
            int flags = quad.getFlags();
            // If the face is aligned, use the light data above it
            // To match vanilla behavior, also treat the face as aligned if it is parallel and the block state is a full cube
            if ((flags & ModelQuadFlags.IS_ALIGNED) != 0 || ((flags & ModelQuadFlags.IS_PARALLEL) != 0 && LightDataAccess.unpackFC(this.lightCache.get(pos)))) {
                lightmap = LightDataAccess.unpackLM(this.lightCache.get(pos, face));
            } else {
                lightmap = LightDataAccess.unpackLM(this.lightCache.get(pos));
            }
        }

        Arrays.fill(out.lm, lightmap);
        Arrays.fill(out.br, this.lightCache.getWorld().getBrightness(face, shade));
    }
}
