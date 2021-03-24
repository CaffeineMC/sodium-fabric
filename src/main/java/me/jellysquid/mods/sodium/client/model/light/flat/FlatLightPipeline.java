package me.jellysquid.mods.sodium.client.model.light.flat;

import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.data.LightDataAccess;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import net.coderbot.iris.pipeline.DirectionalShadingHelper;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Arrays;

import static me.jellysquid.mods.sodium.client.model.light.data.LightDataAccess.unpackLM;

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
            Arrays.fill(out.lm, unpackLM(this.lightCache.get(pos, face)));
        } else {
            Arrays.fill(out.lm, unpackLM(this.lightCache.get(pos)));
        }

        if (DirectionalShadingHelper.shouldDisableDirectionalShading) {
            Arrays.fill(out.br, 1.0F);

            return;
        }

        Arrays.fill(out.br, this.lightCache.getWorld().getBrightness(face, shade));
    }
}
