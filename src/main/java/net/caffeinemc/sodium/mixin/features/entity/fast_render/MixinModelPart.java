package net.caffeinemc.sodium.mixin.features.entity.fast_render;

import net.caffeinemc.sodium.interop.vanilla.mixin.ModelCuboidAccessor;
import net.caffeinemc.sodium.interop.vanilla.vertex.VanillaVertexFormats;
import net.caffeinemc.sodium.render.vertex.VertexDrain;
import net.caffeinemc.sodium.interop.vanilla.vertex.formats.quad.QuadVertexSink;
import net.caffeinemc.sodium.util.packed.Normal3b;
import net.caffeinemc.sodium.util.packed.ColorABGR;
import net.caffeinemc.sodium.interop.vanilla.math.matrix.Matrix3fExtended;
import net.caffeinemc.sodium.interop.vanilla.math.matrix.Matrix4fExtended;
import net.caffeinemc.sodium.interop.vanilla.math.matrix.MatrixUtil;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(ModelPart.class)
public class MixinModelPart {
    private static final float NORM = 1.0F / 16.0F;

    @Shadow
    @Final
    private List<ModelPart.Cuboid> cuboids;

    /**
     * @author JellySquid
     * @reason Use optimized vertex writer, avoid allocations, use quick matrix transformations
     */
    @Overwrite
    private void renderCuboids(MatrixStack.Entry matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        Matrix3fExtended normalExt = MatrixUtil.getExtendedMatrix(matrices.getNormalMatrix());
        Matrix4fExtended positionExt = MatrixUtil.getExtendedMatrix(matrices.getPositionMatrix());

        QuadVertexSink drain = VertexDrain.of(vertexConsumer).createSink(VanillaVertexFormats.QUADS);
        drain.ensureCapacity(this.cuboids.size() * 6 * 4);

        int color = ColorABGR.pack(red, green, blue, alpha);

        for (ModelPart.Cuboid cuboid : this.cuboids) {
            for (ModelPart.Quad quad : ((ModelCuboidAccessor) cuboid).getQuads()) {
                float normX = normalExt.transformVecX(quad.direction);
                float normY = normalExt.transformVecY(quad.direction);
                float normZ = normalExt.transformVecZ(quad.direction);

                int norm = Normal3b.pack(normX, normY, normZ);

                for (ModelPart.Vertex vertex : quad.vertices) {
                    Vec3f pos = vertex.pos;

                    float x1 = pos.getX() * NORM;
                    float y1 = pos.getY() * NORM;
                    float z1 = pos.getZ() * NORM;

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
