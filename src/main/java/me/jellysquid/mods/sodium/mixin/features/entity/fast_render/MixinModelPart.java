package me.jellysquid.mods.sodium.mixin.features.entity.fast_render;

import me.jellysquid.mods.sodium.client.model.ModelCuboidAccessor;
import me.jellysquid.mods.sodium.client.render.vertex.formats.ModelVertex;
import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
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
        var writer = VertexBufferWriter.of(vertexConsumer);
        int color = ColorABGR.pack(red, green, blue, alpha);

        try (MemoryStack stack = VertexBufferWriter.STACK.push()) {
            var buffer = stack.nmalloc(ModelVertex.STRIDE * 4);

            for (ModelPart.Cuboid cuboid : this.cuboids) {
                for (ModelPart.Quad quad : ((ModelCuboidAccessor) cuboid).getQuads()) {
                    Matrix3f normal = matrices.getNormalMatrix();
                    Matrix4f matrix = matrices.getPositionMatrix();

                    float normX = Math.fma(normal.m00(), quad.direction.x, Math.fma(normal.m10(), quad.direction.y, normal.m20() * quad.direction.z));
                    float normY = Math.fma(normal.m01(), quad.direction.x, Math.fma(normal.m11(), quad.direction.y, normal.m21() * quad.direction.z));
                    float normZ = Math.fma(normal.m02(), quad.direction.x, Math.fma(normal.m12(), quad.direction.y, normal.m22() * quad.direction.z));

                    int norm = Norm3b.pack(normX, normY, normZ);

                    var ptr = buffer;

                    for (ModelPart.Vertex vertex : quad.vertices) {
                        Vector3f pos = vertex.pos;

                        float x1 = pos.x() * NORM;
                        float y1 = pos.y() * NORM;
                        float z1 = pos.z() * NORM;

                        float x2 = Math.fma(matrix.m00(), x1, Math.fma(matrix.m10(), y1, Math.fma(matrix.m20(), z1, matrix.m30())));
                        float y2 = Math.fma(matrix.m01(), x1, Math.fma(matrix.m11(), y1, Math.fma(matrix.m21(), z1, matrix.m31())));
                        float z2 = Math.fma(matrix.m02(), x1, Math.fma(matrix.m12(), y1, Math.fma(matrix.m22(), z1, matrix.m32())));

                        ModelVertex.write(ptr, x2, y2, z2, color, vertex.u, vertex.v, light, overlay, norm);

                        ptr += ModelVertex.STRIDE;
                    }

                    writer.push(buffer, 4, ModelVertex.FORMAT);
                }
            }
        }
    }
}
