package me.jellysquid.mods.sodium.client.render.immediate.model;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ModelVertex;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.render.immediate.RenderImmediate;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public class BakedModelEncoder {
    public static void writeQuadVertices(VertexBufferWriter writer, MatrixStack.Entry matrices, ModelQuadView quad, int light, int overlay, int color) {
        Matrix3f matNormal = matrices.getNormalMatrix();
        Matrix4f matPosition = matrices.getPositionMatrix();

        try (MemoryStack stack = RenderImmediate.VERTEX_DATA.push()) {
            long buffer = stack.nmalloc(4 * ModelVertex.STRIDE);
            long ptr = buffer;

            // The packed transformed normal vector
            var normal = MatrixHelper.transformNormal(matNormal, quad.getNormal());

            for (int i = 0; i < 4; i++) {
                // The position vector
                float x = quad.getX(i);
                float y = quad.getY(i);
                float z = quad.getZ(i);

                // The transformed position vector
                float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
                float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
                float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);

                ModelVertex.write(ptr, xt, yt, zt, color, quad.getTexU(i), quad.getTexV(i), overlay, light, normal);
                ptr += ModelVertex.STRIDE;
            }

            writer.push(stack, buffer, 4, ModelVertex.FORMAT);
        }
    }
}
