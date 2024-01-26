package me.jellysquid.mods.sodium.client.render.chunk.vertex.format.impl;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import org.lwjgl.system.MemoryUtil;

public class CompactChunkVertex implements ChunkVertexType {
    public static final GlVertexFormat<ChunkMeshAttribute> VERTEX_FORMAT = GlVertexFormat.builder(ChunkMeshAttribute.class, 20)
            .addElement(ChunkMeshAttribute.POSITION_MATERIAL_MESH, 0, GlVertexAttributeFormat.UNSIGNED_SHORT, 4, false, true)
            .addElement(ChunkMeshAttribute.COLOR_SHADE, 8, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, false)
            .addElement(ChunkMeshAttribute.BLOCK_TEXTURE, 12, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, false)
            .addElement(ChunkMeshAttribute.LIGHT_TEXTURE, 16, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, true)
            .build();

    public static final int STRIDE = 20;

    private static final int POSITION_MAX_VALUE = 65536;
    private static final int TEXTURE_MAX_VALUE = 32768;

    private static final float MODEL_ORIGIN = 8.0f;
    private static final float MODEL_RANGE = 32.0f;
    private static final float MODEL_SCALE = MODEL_RANGE / POSITION_MAX_VALUE;
    private static final float MODEL_SCALE_INV = POSITION_MAX_VALUE / MODEL_RANGE;

    private static final float TEXTURE_SCALE = (1.0f / TEXTURE_MAX_VALUE);

    @Override
    public float getTextureScale() {
        return TEXTURE_SCALE;
    }

    @Override
    public float getPositionScale() {
        return MODEL_SCALE;
    }

    @Override
    public float getPositionOffset() {
        return -MODEL_ORIGIN;
    }

    @Override
    public GlVertexFormat<ChunkMeshAttribute> getVertexFormat() {
        return VERTEX_FORMAT;
    }

    @Override
    public ChunkVertexEncoder getEncoder() {
        return (ptr, material, vertex, sectionIndex) -> {
            MemoryUtil.memPutShort(ptr + 0, encodePosition(vertex.x));
            MemoryUtil.memPutShort(ptr + 2, encodePosition(vertex.y));
            MemoryUtil.memPutShort(ptr + 4, encodePosition(vertex.z));

            MemoryUtil.memPutByte(ptr + 6, (byte) (material.bits() & 0xFF));
            MemoryUtil.memPutByte(ptr + 7, (byte) (sectionIndex & 0xFF));

            MemoryUtil.memPutInt(ptr + 8, vertex.color);

            MemoryUtil.memPutShort(ptr + 12, encodeTexture(vertex.u));
            MemoryUtil.memPutShort(ptr + 14, encodeTexture(vertex.v));

            MemoryUtil.memPutInt(ptr + 16, vertex.light);

            return ptr + STRIDE;
        };
    }

    private static short encodePosition(float v) {
        return (short) ((MODEL_ORIGIN + v) * MODEL_SCALE_INV);
    }

    private static short encodeTexture(float value) {
        return (short) (Math.round(value * TEXTURE_MAX_VALUE) & 0xFFFF);
    }
}
