package me.jellysquid.mods.sodium.client.render.vertex;

import me.jellysquid.mods.sodium.client.render.vertex.buffer.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexElementType;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.common.util.UnsafeUtil;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import static me.jellysquid.mods.sodium.client.render.vertex.VertexElementSerializer.*;

public class VertexTransformers {
    /**
     * Transforms the texture UVs for each vertex from their absolute coordinates into the sprite area specified
     * by the parameters.
     *
     * @param ptr    The buffer of vertices to transform
     * @param count  The number of vertices to transform
     * @param format The format of the vertices
     * @param minU   The minimum X-coordinate of the sprite bounds
     * @param minV   The minimum Y-coordinate of the sprite bounds
     * @param maxU   The maximum X-coordinate of the sprite bounds
     * @param maxV   The maximum Y-coordinate of the sprite bounds
     */
    public static void transformSprite(long ptr, int count, VertexFormatDescription format,
                                       float minU, float minV, float maxU, float maxV) {
        long stride = format.stride;
        long offsetUV = format.getElementOffset(VertexElementType.TEXTURE);

        // The width/height of the sprite
        float w = maxU - minU;
        float h = maxV - minV;

        for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
            // The texture coordinates relative to the sprite bounds
            float u = getTextureU(ptr + offsetUV);
            float v = getTextureV(ptr + offsetUV);

            // The texture coordinates in absolute space on the sprite sheet
            float ut = minU + (w * u);
            float vt = minV + (h * v);

            setTextureUV(ptr + offsetUV, ut, vt);

            ptr += stride;
        }
    }

    /**
     * Transforms the color element of each vertex to use the specified value.
     *
     * @param ptr    The buffer of vertices to transform
     * @param count  The number of vertices to transform
     * @param format The format of the vertices
     * @param color  The packed color to use for transforming the vertices
     */
    public static void transformColor(long ptr, int count, VertexFormatDescription format,
                                      int color) {
        var offsetColor = format.getElementOffset(VertexElementType.COLOR);

        for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
            setColorABGR(ptr + offsetColor, color);
            ptr += format.stride;
        }
    }

    /**
     * Transforms the overlay UVs element of each vertex to create a perspective-mapped effect.
     *
     * @param ptr    The buffer of vertices to transform
     * @param count  The number of vertices to transform
     * @param format The format of the vertices
     * @param inverseNormalMatrix The inverted normal matrix
     * @param inverseTextureMatrix The inverted texture matrix
     * @param textureScale The amount which the overlay texture should be adjusted
     */
    public static void transformOverlay(long ptr, int count, VertexFormatDescription format,
                                        Matrix3f inverseNormalMatrix, Matrix4f inverseTextureMatrix, float textureScale) {
        var offsetPosition = format.getElementOffset(VertexElementType.POSITION);
        var offsetColor = format.getElementOffset(VertexElementType.COLOR);
        var offsetNormal = format.getElementOffset(VertexElementType.NORMAL);
        var offsetTexture = format.getElementOffset(VertexElementType.TEXTURE);

        int color = ColorABGR.pack(1.0f, 1.0f, 1.0f, 1.0f);

        var normal = new Vector3f(Float.NaN);
        var position = new Vector4f(Float.NaN);

        for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
            position.x = MemoryUtil.memGetFloat(ptr + offsetPosition + 0);
            position.y = MemoryUtil.memGetFloat(ptr + offsetPosition + 4);
            position.z = MemoryUtil.memGetFloat(ptr + offsetPosition + 8);
            position.w = 1.0f;

            int packedNormal = MemoryUtil.memGetInt(ptr + offsetNormal);
            normal.x = Norm3b.unpackX(packedNormal);
            normal.y = Norm3b.unpackY(packedNormal);
            normal.z = Norm3b.unpackZ(packedNormal);

            Vector3f transformedNormal = inverseNormalMatrix.transform(normal);
            Direction direction = Direction.getFacing(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());

            Vector4f transformedTexture = inverseTextureMatrix.transform(position);
            transformedTexture.rotateY(3.1415927F);
            transformedTexture.rotateX(-1.5707964F);
            transformedTexture.rotate(direction.getRotationQuaternion());

            float textureU = -transformedTexture.x() * textureScale;
            float textureV = -transformedTexture.y() * textureScale;

            setColorABGR(ptr + offsetColor, color);
            setTextureUV(ptr + offsetTexture, textureU, textureV);

            ptr += format.stride;
        }
    }

    /**
     * Creates a copy of the source data and pushes it into the specified {@param consumer}. This is useful for when
     * you need to use to re-use the source data after the call, and do not want the {@param consumer} to modify
     * the original data.
     *
     * @param consumer The vertex consumer to push the vertices into
     * @param stack    The memory stack which can be used to create temporary allocations
     * @param ptr      The pointer to read vertices from
     * @param count    The number of vertices to push
     * @param format   The format of the vertices
     */
    public static void copyPush(VertexConsumer consumer, MemoryStack stack, long ptr, int count, VertexFormatDescription format) {
        var length = count * format.stride;
        var copy = stack.nmalloc(length);

        UnsafeUtil.copyMemory(ptr, copy, length);

        VertexBufferWriter.of(consumer)
                .push(stack, copy, count, format);
    }
}
