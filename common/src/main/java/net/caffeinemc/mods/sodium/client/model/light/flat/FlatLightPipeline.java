package net.caffeinemc.mods.sodium.client.model.light.flat;

import net.caffeinemc.mods.sodium.client.model.light.LightPipeline;
import net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.Arrays;

import static net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess.*;

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
    public void calculate(ModelQuadView quad, BlockPos pos, QuadLightData out, Direction cullFace, Direction lightFace, boolean shade, boolean enhanced) {
        int lightmap;

        // To match vanilla behavior, use the cull face if it exists/is available
        if (cullFace != null) {
            lightmap = getOffsetLightmap(pos, cullFace);
            Arrays.fill(out.br, this.lightCache.getLevel().getShade(lightFace, shade));
        } else {
            int flags = quad.getFlags();
            // If the face is aligned, use the light data above it
            // To match vanilla behavior, also treat the face as aligned if it is parallel and the block state is a full cube
            if ((flags & ModelQuadFlags.IS_ALIGNED) != 0 || ((flags & ModelQuadFlags.IS_PARALLEL) != 0 && unpackFC(this.lightCache.get(pos)))) {
                lightmap = getOffsetLightmap(pos, lightFace);
                Arrays.fill(out.br, this.lightCache.getLevel().getShade(lightFace, shade));
            } else {
                lightmap = getEmissiveLightmap(this.lightCache.get(pos));
                Arrays.fill(out.br, enhanced ? PlatformBlockAccess.getInstance().getNormalVectorShade(quad, this.lightCache.getLevel(), shade) : this.lightCache.getLevel().getShade(lightFace, shade));
            }
        }

        Arrays.fill(out.lm, lightmap);
    }

    /**
     * When vanilla computes an offset lightmap with flat lighting, it passes the original BlockState but the
     * offset BlockPos to {@link LevelRenderer#getLightColor(BlockAndTintGetter, BlockState, BlockPos)}.
     * This does not make much sense but fixes certain issues, primarily dark quads on light-emitting blocks
     * behind tinted glass. {@link LightDataAccess} cannot efficiently store lightmaps computed with
     * inconsistent values so this method exists to mirror vanilla behavior as closely as possible.
     */
    private int getOffsetLightmap(BlockPos pos, Direction face) {
        int word = this.lightCache.get(pos);

        // Check emissivity of the origin state
        if (unpackEM(word)) {
            return LightTexture.FULL_BRIGHT;
        }

        // Use light values from the offset pos, but luminance from the origin pos
        int adjWord = this.lightCache.get(pos, face);
        return LightTexture.pack(Math.max(unpackBL(adjWord), unpackLU(word)), unpackSL(adjWord));
    }
}
