package me.jellysquid.mods.sodium.mixin.features.entity.fast_render;

import me.jellysquid.mods.sodium.interop.vanilla.mixin.ModelCubeAccessor;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.VanillaVertexFormats;
import me.jellysquid.mods.sodium.render.vertex.VertexDrain;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.quad.QuadVertexSink;
import me.jellysquid.mods.sodium.util.packed.Normal3b;
import net.minecraft.client.model.geom.ModelPart;
import me.jellysquid.mods.sodium.util.packed.ColorABGR;
import me.jellysquid.mods.sodium.interop.vanilla.math.matrix.Matrix3fExtended;
import me.jellysquid.mods.sodium.interop.vanilla.math.matrix.Matrix4fExtended;
import me.jellysquid.mods.sodium.interop.vanilla.math.matrix.MatrixUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import java.util.List;

@Mixin(ModelPart.class)
public class MixinModelPart {
    private static final float NORM = 1.0F / 16.0F;

    @Shadow
    @Final
    private List<ModelPart.Cube> cubes;

    /**
     * @author JellySquid
     * @reason Use optimized vertex writer, avoid allocations, use quick matrix transformations
     */
    @Overwrite
    private void compile(PoseStack.Pose matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        Matrix3fExtended normalExt = MatrixUtil.getExtendedMatrix(matrices.normal());
        Matrix4fExtended positionExt = MatrixUtil.getExtendedMatrix(matrices.pose());

        QuadVertexSink drain = VertexDrain.of(vertexConsumer).createSink(VanillaVertexFormats.QUADS);
        drain.ensureCapacity(this.cubes.size() * 6 * 4);

        int color = ColorABGR.pack(red, green, blue, alpha);

        for (ModelPart.Cube cuboid : this.cubes) {
            for (ModelPart.Polygon quad : ((ModelCubeAccessor) cuboid).getQuads()) {
                float normX = normalExt.transformVecX(quad.normal);
                float normY = normalExt.transformVecY(quad.normal);
                float normZ = normalExt.transformVecZ(quad.normal);

                int norm = Normal3b.pack(normX, normY, normZ);

                for (ModelPart.Vertex vertex : quad.vertices) {
                    Vector3f pos = vertex.pos;

                    float x1 = pos.x() * NORM;
                    float y1 = pos.y() * NORM;
                    float z1 = pos.z() * NORM;

                    float x2 = positionExt.transformVecX(x1, y1, z1);
                    float y2 = positionExt.transformVecY(x1, y1, z1);
                    float z2 = positionExt.transformVecZ(x1, y1, z1);

                    drain.writeQuad(x2, y2, z2, color, vertex.u, vertex.v, light, overlay, norm);
                }
            }
        }

        drain.flush();
    }
}
