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
    public void calculate(ModelQuadView quad, BlockPos pos, QuadLightData out, Direction face, boolean shade) {
        // If the face is aligned, use the light data above it
        if ((quad.getFlags() & ModelQuadFlags.IS_ALIGNED) != 0 && !this.lightCache.getWorld().getBlockState(pos).hasEmissiveLighting(this.lightCache.getWorld(), pos)) {
            Arrays.fill(out.lm, LightDataAccess.unpackLM(this.lightCache.get(pos, face)));
        } else {
            Arrays.fill(out.lm, LightDataAccess.unpackLM(this.lightCache.get(pos)));
        }

        Arrays.fill(out.br, this.lightCache.getWorld().getBrightness(face, shade));
    }
}
