package net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl;

import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexFormat;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkMeshAttribute;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.minecraft.util.Mth;
import org.lwjgl.system.MemoryUtil;

public class CompactChunkVertex implements ChunkVertexType {
    public static final int STRIDE = 20;

    public static final GlVertexFormat<ChunkMeshAttribute> VERTEX_FORMAT = GlVertexFormat.builder(ChunkMeshAttribute.class, STRIDE)
            .addElement(ChunkMeshAttribute.POSITION_HI, 0, GlVertexAttributeFormat.UNSIGNED_2_10_10_10_REV, 4, false, false)
            .addElement(ChunkMeshAttribute.POSITION_LO, 4, GlVertexAttributeFormat.UNSIGNED_2_10_10_10_REV, 4, false, false)
            .addElement(ChunkMeshAttribute.COLOR, 8, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, false)
            .addElement(ChunkMeshAttribute.TEXTURE, 12, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, true)
            .addElement(ChunkMeshAttribute.LIGHT_MATERIAL_INDEX, 16, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, false, true)
            .build();

    private static final int POSITION_MAX_VALUE = 1 << 20;
    private static final int TEXTURE_MAX_VALUE = 1 << 15;

    private static final float MODEL_TRANSLATION = 8.0f;
    private static final float MODEL_SCALE = 32.0f;

    @Override
    public GlVertexFormat<ChunkMeshAttribute> getVertexFormat() {
        return VERTEX_FORMAT;
    }

    @Override
    public ChunkVertexEncoder getEncoder() {
        return (ptr, material, vertices, section) -> {
            // Calculate the center point of the texture region which is mapped to the quad
            float texCentroidU = 0.0f;
            float texCentroidV = 0.0f;

            for (var vertex : vertices) {
                texCentroidU += vertex.u;
                texCentroidV += vertex.v;
            }

            texCentroidU *= (1.0f / 4.0f);
            texCentroidV *= (1.0f / 4.0f);

            for (int i = 0; i < 4; i++) {
                var vertex = vertices[i];

                int x = encodePosition(vertex.x);
                int y = encodePosition(vertex.y);
                int z = encodePosition(vertex.z);

                int u = encodeTexture(texCentroidU, vertex.u);
                int v = encodeTexture(texCentroidV, vertex.v);

                int light = encodeLight(vertex.light);

                MemoryUtil.memPutInt(ptr +  0L, packPositionHi(x, y, z));
                MemoryUtil.memPutInt(ptr +  4L, packPositionLo(x, y, z));
                MemoryUtil.memPutInt(ptr +  8L, vertex.color);
                MemoryUtil.memPutInt(ptr + 12L, packTexture(u, v));
                MemoryUtil.memPutInt(ptr + 16L, packLightAndData(light, material.bits(), section));

                ptr += STRIDE;
            }

            return ptr;
        };
    }

    private static int packPositionHi(int x, int y, int z) {
        return  (((x >>> 10) & 0x3FF) <<  0) |
                (((y >>> 10) & 0x3FF) << 10) |
                (((z >>> 10) & 0x3FF) << 20);
    }

    private static int packPositionLo(int x, int y, int z) {
        return  ((x & 0x3FF) <<  0) |
                ((y & 0x3FF) << 10) |
                ((z & 0x3FF) << 20);
    }

    private static int encodePosition(float position) {
        return Math.round((position + MODEL_TRANSLATION) * (POSITION_MAX_VALUE / MODEL_SCALE));
    }

    private static int packTexture(int u, int v) {
        return ((u & 0xFFFF) << 0) | ((v & 0xFFFF) << 16);
    }

    private static int encodeTexture(float center, float x) {
        // Normally, the UV coordinates are shrunk towards the center of the texture by a very small epsilon to avoid
        // texture bleeding between sprites in the atlas. When compressing to smaller integer formats, this creates a
        // problem, since the very small offsets can not be encoded without using a significant number of bits.
        //
        // Rather than encoding very small epsilons, we use a single bit to represent the sign of a constant bias
        // value, which is then applied in the shader code. The sign of the bias is determined by comparing the
        // coordinates to the center point of the texture region.
        //
        // By always shrinking the texture coordinates at least one unit, we also avoid needing to encode both x=0.0 and
        // x=1.0, which normally requires an extra bit to represent.
        int bias = (x < center) ? 1 : -1;
        int quantized = floorInt(x * TEXTURE_MAX_VALUE) + bias;

        // When packing the values into a 16-bit unsigned integer, the 15-bit quantized value is shifted to the left by
        // one, and the least-significant bit (which is now zero-filled) is used to store the sign of the bias.
        return ((quantized & 0x7FFF) << 1) | (bias >>> 31);
    }

    private static int encodeLight(int light) {
        int sky = Mth.clamp((light >>> 16) & 0xFF, 8, 248);
        int block = Mth.clamp((light >>>  0) & 0xFF, 8, 248);

        return (block << 0) | (sky << 8);
    }

    private static int packLightAndData(int light, int material, int section) {
        return ((light & 0xFFFF) << 0) |
                ((material & 0xFF) << 16) |
                ((section & 0xFF) << 24);
    }

    private static int floorInt(float x) {
        return (int) Math.floor(x);
    }
}
