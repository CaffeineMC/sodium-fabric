package net.caffeinemc.mods.sodium.client.render.frapi.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.vertex.VertexConsumerUtils;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.EntityVertex;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

public class QuadEncoder {
    public static void writeQuadVertices(MutableQuadViewImpl quad, VertexConsumer vertexConsumer, int overlay, Matrix4f matPosition, boolean trustedNormals, Matrix3f matNormal) {
        VertexBufferWriter writer = VertexConsumerUtils.convertOrLog(vertexConsumer);

        if (writer != null) {
            writeQuadVertices(quad, writer, overlay, matPosition, trustedNormals, matNormal);
        } else {
            writeQuadVerticesSlow(quad, vertexConsumer, overlay, matPosition, trustedNormals, matNormal);
        }
    }

    public static void writeQuadVertices(MutableQuadViewImpl quad, VertexBufferWriter writer, int overlay, Matrix4f matPosition, boolean trustedNormals, Matrix3f matNormal) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * EntityVertex.STRIDE);
            long ptr = buffer;

            final boolean useNormals = quad.hasVertexNormals();

            // The packed transformed normal vector
            int normal = 0;

            if (useNormals) {
                quad.populateMissingNormals();
            } else {
                normal = MatrixHelper.transformNormal(matNormal, trustedNormals, quad.packedFaceNormal());
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
                    normal = MatrixHelper.transformNormal(matNormal, trustedNormals, quad.packedNormal(i));
                }

                EntityVertex.write(ptr, xt, yt, zt, ColorARGB.toABGR(quad.color(i)), quad.u(i), quad.v(i), overlay, quad.lightmap(i), normal);
                ptr += EntityVertex.STRIDE;
            }

            writer.push(stack, buffer, 4, EntityVertex.FORMAT);
        }
    }

    private static void writeQuadVerticesSlow(MutableQuadViewImpl quad, VertexConsumer vertexConsumer, int overlay, Matrix4f matPosition, boolean trustedNormals, Matrix3f matNormal) {
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

            if (!trustedNormals) {
                float scalar = Math.invsqrt(Math.fma(nxt, nxt, Math.fma(nyt, nyt, nzt * nzt)));

                nxt *= scalar;
                nyt *= scalar;
                nzt *= scalar;
            }
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

            vertexConsumer.addVertex(xt, yt, zt);

            vertexConsumer.setColor(quad.color(i));
            vertexConsumer.setUv(quad.u(i), quad.v(i));
            vertexConsumer.setOverlay(overlay);
            vertexConsumer.setLight(quad.lightmap(i));

            if (useNormals) {
                int packedNormal = quad.packedNormal(i);

                // The normal vector
                float nx = NormI8.unpackX(packedNormal);
                float ny = NormI8.unpackY(packedNormal);
                float nz = NormI8.unpackZ(packedNormal);

                nxt = MatrixHelper.transformNormalX(matNormal, nx, ny, nz);
                nyt = MatrixHelper.transformNormalY(matNormal, nx, ny, nz);
                nzt = MatrixHelper.transformNormalZ(matNormal, nx, ny, nz);

                if (!trustedNormals) {
                    float scalar = Math.invsqrt(Math.fma(nxt, nxt, Math.fma(nyt, nyt, nzt * nzt)));

                    nxt *= scalar;
                    nyt *= scalar;
                    nzt *= scalar;
                }
            }

            vertexConsumer.setNormal(nxt, nyt, nzt);
        }
    }
}
