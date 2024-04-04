package net.caffeinemc.mods.sodium.client.render.frapi.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.vertex.VertexConsumerUtils;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ModelVertex;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

public class QuadEncoder {
    public static void writeQuadVertices(MutableQuadViewImpl quad, VertexConsumer vertexConsumer, int overlay, Matrix4f matPosition, Matrix3f matNormal) {
        VertexBufferWriter writer = VertexConsumerUtils.convertOrLog(vertexConsumer);

        if (writer != null) {
            writeQuadVertices(quad, writer, overlay, matPosition, matNormal);
        } else {
            writeQuadVerticesSlow(quad, vertexConsumer, overlay, matPosition, matNormal);
        }
    }

    public static void writeQuadVertices(MutableQuadViewImpl quad, VertexBufferWriter writer, int overlay, Matrix4f matPosition, Matrix3f matNormal) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * ModelVertex.STRIDE);
            long ptr = buffer;

            final boolean useNormals = quad.hasVertexNormals();

            // The packed transformed normal vector
            int normal = 0;

            if (useNormals) {
                quad.populateMissingNormals();
            } else {
                normal = MatrixHelper.transformNormal(matNormal, quad.packedFaceNormal());
            }

            for (int i = 0; i < 4; i++) {
                // The position vector
                float x = quad.x(i);
                float y = quad.y(i);
                float z = quad.z(i);

                // The transformed position vector
                float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
                float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
                float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);

                if (useNormals) {
                    normal = MatrixHelper.transformNormal(matNormal, quad.packedNormal(i));
                }

                ModelVertex.write(ptr, xt, yt, zt, ColorARGB.toABGR(quad.color(i)), quad.u(i), quad.v(i), overlay, quad.lightmap(i), normal);
                ptr += ModelVertex.STRIDE;
            }

            writer.push(stack, buffer, 4, ModelVertex.FORMAT);
        }
    }

    private static void writeQuadVerticesSlow(MutableQuadViewImpl quad, VertexConsumer vertexConsumer, int overlay, Matrix4f matPosition, Matrix3f matNormal) {
        final boolean useNormals = quad.hasVertexNormals();

        // The transformed normal vector
        float nxt = 0;
        float nyt = 0;
        float nzt = 0;

        if (useNormals) {
            quad.populateMissingNormals();
        } else {
            Vector3f faceNormal = quad.faceNormal();

            // The normal vector
            float nx = faceNormal.x;
            float ny = faceNormal.y;
            float nz = faceNormal.z;

            nxt = MatrixHelper.transformNormalX(matNormal, nx, ny, nz);
            nyt = MatrixHelper.transformNormalY(matNormal, nx, ny, nz);
            nzt = MatrixHelper.transformNormalZ(matNormal, nx, ny, nz);
        }

        for (int i = 0; i < 4; i++) {
            // The position vector
            float x = quad.x(i);
            float y = quad.y(i);
            float z = quad.z(i);

            // The transformed position vector
            float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
            float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
            float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);

            vertexConsumer.vertex(xt, yt, zt);

            vertexConsumer.color(quad.color(i));
            vertexConsumer.uv(quad.u(i), quad.v(i));
            vertexConsumer.overlayCoords(overlay);
            vertexConsumer.uv2(quad.lightmap(i));

            if (useNormals) {
                int packedNormal = quad.packedNormal(i);

                // The normal vector
                float nx = NormI8.unpackX(packedNormal);
                float ny = NormI8.unpackY(packedNormal);
                float nz = NormI8.unpackZ(packedNormal);

                nxt = MatrixHelper.transformNormalX(matNormal, nx, ny, nz);
                nyt = MatrixHelper.transformNormalY(matNormal, nx, ny, nz);
                nzt = MatrixHelper.transformNormalZ(matNormal, nx, ny, nz);
            }

            vertexConsumer.normal(nxt, nyt, nzt);
            vertexConsumer.endVertex();
        }
    }
}
