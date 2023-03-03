package me.jellysquid.mods.sodium.mixin.features.model;

import me.jellysquid.mods.sodium.client.model.quad.BakedQuadOcclusionAccessor;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BasicBakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(BasicBakedModel.class)
public class MixinBasicBakedModel {

    private static Vector3f getVertexPosition(int[] data, int vertex) {
        return new Vector3f(Float.intBitsToFloat(data[vertex * 8]),
                Float.intBitsToFloat(data[vertex * 8 + 1]),
                Float.intBitsToFloat(data[vertex * 8 + 2]));
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void fixFaces(List<BakedQuad> quads, Map<Direction, List<BakedQuad>> faceQuads, boolean usesAo, boolean isSideLit, boolean hasDepth, Sprite sprite, ModelTransformation transformation, ModelOverrideList itemPropertyOverrides, CallbackInfo ci) {
        //Sorts if possible quads into faceQuads based on the computed normals
        Iterator<BakedQuad> quadIterator = quads.listIterator();
        while (quadIterator.hasNext()) {
            BakedQuad quad = quadIterator.next();

            int[] vertexData = quad.getVertexData();
            Vector3f base = getVertexPosition(vertexData, 0);
            Vector3f a = getVertexPosition(vertexData, 1).sub(base);
            Vector3f b = getVertexPosition(vertexData, 3).sub(base);
            Vector3f normal = a.cross(b).normalize();//Generate the normal vector from the positions

            //Compute the best matching direction for the quad
            float match = 0;
            Direction best = null;
            for (Direction i : DirectionUtil.ALL_DIRECTIONS) {
                float diff = normal.dot(i.getUnitVector());
                if (match < diff) {
                    match = diff;
                    best = i;
                }
            }

            //If its close enough to being on a direction, snap to the direction, reassign the quad
            // and mark as having no occlusion
            if (match>0.999999) {
                quadIterator.remove();
                ((BakedQuadOcclusionAccessor)quad).setNoOcclusion(true);
                faceQuads.computeIfAbsent(best, (direction)->new ArrayList<>()).add(quad);
            }
        }
    }
}
