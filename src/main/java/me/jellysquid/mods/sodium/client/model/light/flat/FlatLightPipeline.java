package me.jellysquid.mods.sodium.client.model.light.flat;

import me.jellysquid.mods.sodium.client.frapi.mesh.QuadViewImpl;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.data.LightDataAccess;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.joml.Vector3f;

import java.util.Arrays;

import static me.jellysquid.mods.sodium.client.model.light.data.LightDataAccess.*;

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
    public void calculate(ModelQuadView modelQuadView, BlockPos pos, QuadLightData out, Direction cullFace, Direction lightFace, boolean shade) {
        int lightmap;

        // To match vanilla behavior, use the cull face if it exists/is available
        if (cullFace != null) {
            lightmap = getOffsetLightmap(pos, cullFace);
        } else {
            int flags = modelQuadView.getFlags();
            // If the face is aligned, use the light data above it
            // To match vanilla behavior, also treat the face as aligned if it is parallel and the block state is a full cube
            if ((flags & ModelQuadFlags.IS_ALIGNED) != 0 || ((flags & ModelQuadFlags.IS_PARALLEL) != 0 && unpackFC(this.lightCache.get(pos)))) {
                lightmap = getOffsetLightmap(pos, lightFace);
            } else {
                lightmap = getEmissiveLightmap(this.lightCache.get(pos));
            }
        }

        Arrays.fill(out.lm, lightmap);

        /*
         * Compute brightness depending on quad normal. For irregular quads this is essential to achieve decent results.
         * This code comes from Indigo, which is licensed under the Apache 2.0 license to FabricMC.
         */
        // TODO: this cast is quite ugly, what should we do? Move this to AbstractBlockRenderContext?
        if (modelQuadView instanceof QuadViewImpl quad) {
            BlockRenderView blockView = this.lightCache.getWorld();

            if (quad.hasAllVertexNormals()) {
                for (int i = 0; i < 4; i++) {
                    out.br[i] = normalShade(blockView, quad.normalX(i), quad.normalY(i), quad.normalZ(i), shade);
                }
            } else {
                final float faceShade;

                if ((quad.geometryFlags() & ModelQuadFlags.IS_ALIGNED) != 0) {
                    faceShade = blockView.getBrightness(lightFace, shade);
                } else {
                    Vector3f faceNormal = quad.faceNormal();
                    faceShade = normalShade(blockView, faceNormal.x, faceNormal.y, faceNormal.z, shade);
                }

                if (quad.hasVertexNormals()) {
                    for (int i = 0; i < 4; i++) {
                        if (quad.hasNormal(i)) {
                            out.br[i] = normalShade(blockView, quad.normalX(i), quad.normalY(i), quad.normalZ(i), shade);
                        } else {
                            out.br[i] = faceShade;
                        }
                    }
                } else {
                    Arrays.fill(out.br, faceShade);
                }
            }
        } else {
            Arrays.fill(out.br, this.lightCache.getWorld().getBrightness(lightFace, shade));
        }
    }

    /**
     * When vanilla computes an offset lightmap with flat lighting, it passes the original BlockState but the
     * offset BlockPos to {@link WorldRenderer#getLightmapCoordinates(BlockRenderView, BlockState, BlockPos)}.
     * This does not make much sense but fixes certain issues, primarily dark quads on light-emitting blocks
     * behind tinted glass. {@link LightDataAccess} cannot efficiently store lightmaps computed with
     * inconsistent values so this method exists to mirror vanilla behavior as closely as possible.
     */
    private int getOffsetLightmap(BlockPos pos, Direction face) {
        int word = this.lightCache.get(pos);

        // Check emissivity of the origin state
        if (unpackEM(word)) {
            return LightmapTextureManager.MAX_LIGHT_COORDINATE;
        }

        // Use world light values from the offset pos, but luminance from the origin pos
        int adjWord = this.lightCache.get(pos, face);
        return LightmapTextureManager.pack(Math.max(unpackBL(adjWord), unpackLU(word)), unpackSL(adjWord));
    }


    /**
     * Finds mean of per-face shading factors weighted by normal components.
     * Not how light actually works but the vanilla diffuse shading model is a hack to start with
     * and this gives reasonable results for non-cubic surfaces in a vanilla-style renderer.
     * <p>
     * This code comes from Indigo, which is licensed under the Apache 2.0 license to FabricMC.
     */
    private float normalShade(BlockRenderView blockView, float normalX, float normalY, float normalZ, boolean hasShade) {
        float sum = 0;
        float div = 0;

        if (normalX > 0) {
            sum += normalX * blockView.getBrightness(Direction.EAST, hasShade);
            div += normalX;
        } else if (normalX < 0) {
            sum += -normalX * blockView.getBrightness(Direction.WEST, hasShade);
            div -= normalX;
        }

        if (normalY > 0) {
            sum += normalY * blockView.getBrightness(Direction.UP, hasShade);
            div += normalY;
        } else if (normalY < 0) {
            sum += -normalY * blockView.getBrightness(Direction.DOWN, hasShade);
            div -= normalY;
        }

        if (normalZ > 0) {
            sum += normalZ * blockView.getBrightness(Direction.SOUTH, hasShade);
            div += normalZ;
        } else if (normalZ < 0) {
            sum += -normalZ * blockView.getBrightness(Direction.NORTH, hasShade);
            div -= normalZ;
        }

        return sum / div;
    }
}
