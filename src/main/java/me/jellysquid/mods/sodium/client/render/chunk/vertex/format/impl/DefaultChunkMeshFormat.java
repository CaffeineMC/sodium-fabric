package me.jellysquid.mods.sodium.client.render.chunk.vertex.format.impl;

import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ModelQuadEncoder;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ModelQuadFormat;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import org.lwjgl.system.MemoryUtil;

public class DefaultChunkMeshFormat implements ModelQuadFormat {

    public static final int STRIDE = 20 * 4;

    private static final int POSITION_MAX_VALUE = 65536;
    private static final int TEXTURE_MAX_VALUE = 65536;

    private static final float MODEL_ORIGIN = 8.0f;
    private static final float MODEL_SCALE = 32.0f;

    @Override
    public ModelQuadEncoder getEncoder() {
        return (ptr, material, vertices, sectionIndex) -> {
            MemoryUtil.memSet(ptr, 0, STRIDE);

            MemoryUtil.memPutInt(ptr +  0, packU16x2_hi(encodePosition(vertices[3].x), encodePosition(vertices[2].x), encodePosition(vertices[1].x), encodePosition(vertices[0].x)));
            MemoryUtil.memPutInt(ptr +  4, packU16x2_hi(encodePosition(vertices[3].y), encodePosition(vertices[2].y), encodePosition(vertices[1].y), encodePosition(vertices[0].y)));
            MemoryUtil.memPutInt(ptr +  8, packU16x2_hi(encodePosition(vertices[3].z), encodePosition(vertices[2].z), encodePosition(vertices[1].z), encodePosition(vertices[0].z)));

            MemoryUtil.memPutInt(ptr + 16, packU16x2_lo(encodePosition(vertices[3].x), encodePosition(vertices[2].x), encodePosition(vertices[1].x), encodePosition(vertices[0].x)));
            MemoryUtil.memPutInt(ptr + 20, packU16x2_lo(encodePosition(vertices[3].y), encodePosition(vertices[2].y), encodePosition(vertices[1].y), encodePosition(vertices[0].y)));
            MemoryUtil.memPutInt(ptr + 24, packU16x2_lo(encodePosition(vertices[3].z), encodePosition(vertices[2].z), encodePosition(vertices[1].z), encodePosition(vertices[0].z)));

            MemoryUtil.memPutInt(ptr + 32, packU8x4(ColorABGR.unpackRed(vertices[0].color), ColorABGR.unpackRed(vertices[1].color), ColorABGR.unpackRed(vertices[2].color), ColorABGR.unpackRed(vertices[3].color)));
            MemoryUtil.memPutInt(ptr + 36, packU8x4(ColorABGR.unpackGreen(vertices[0].color), ColorABGR.unpackGreen(vertices[1].color), ColorABGR.unpackGreen(vertices[2].color), ColorABGR.unpackGreen(vertices[3].color)));
            MemoryUtil.memPutInt(ptr + 40, packU8x4(ColorABGR.unpackBlue(vertices[0].color), ColorABGR.unpackBlue(vertices[1].color), ColorABGR.unpackBlue(vertices[2].color), ColorABGR.unpackBlue(vertices[3].color)));
            MemoryUtil.memPutInt(ptr + 44, packU8x4(ColorABGR.unpackAlpha(vertices[0].color), ColorABGR.unpackAlpha(vertices[1].color), ColorABGR.unpackAlpha(vertices[2].color), ColorABGR.unpackAlpha(vertices[3].color)));

            MemoryUtil.memPutInt(ptr + 48, packU16x2_hi(encodeTexture(vertices[3].u), encodeTexture(vertices[2].u), encodeTexture(vertices[1].u), encodeTexture(vertices[0].u)));
            MemoryUtil.memPutInt(ptr + 52, packU16x2_hi(encodeTexture(vertices[3].v), encodeTexture(vertices[2].v), encodeTexture(vertices[1].v), encodeTexture(vertices[0].v)));

            MemoryUtil.memPutInt(ptr + 56, packU16x2_lo(encodeTexture(vertices[3].u), encodeTexture(vertices[2].u), encodeTexture(vertices[1].u), encodeTexture(vertices[0].u)));
            MemoryUtil.memPutInt(ptr + 60, packU16x2_lo(encodeTexture(vertices[3].v), encodeTexture(vertices[2].v), encodeTexture(vertices[1].v), encodeTexture(vertices[0].v)));

            MemoryUtil.memPutInt(ptr + 64, packU16x2(encodeLight(vertices[0].light), encodeLight(vertices[1].light)));
            MemoryUtil.memPutInt(ptr + 68, packU16x2(encodeLight(vertices[2].light), encodeLight(vertices[3].light)));

            MemoryUtil.memPutInt(ptr + 72, material.bits());
            MemoryUtil.memPutInt(ptr + 76, sectionIndex);

            return ptr + STRIDE;
        };
    }

    private static int encodeLight(int light) {
        return ((light >> 0) & 0xFF) |
                ((light >> 16) & 0xFF) << 8;
    }

    private static int packU16x2(int a, int b) {
        return ((a & 0xFFFF) << 0) | ((b & 0xFFFF) << 16);
    }

    private static int packU16x2_hi(int x, int y, int w, int z) {
        return  (((x & 0xFF00) >> 8) << 24) |
                (((y & 0xFF00) >> 8) << 16) |
                (((w & 0xFF00) >> 8) <<  8) |
                (((z & 0xFF00) >> 8) <<  0);
    }

    private static int packU16x2_lo(int x, int y, int w, int z) {
        return  ((x & 0x00FF) << 24) |
                ((y & 0x00FF) << 16) |
                ((w & 0x00FF) <<  8) |
                ((z & 0x00FF) <<  0);
    }

    private static int packU8x4(int x, int y, int w, int z) {
        return  ((x & 0xFF) << 24) |
                ((y & 0xFF) << 16) |
                ((w & 0xFF) <<  8) |
                ((z & 0xFF) <<  0);
    }

    @Override
    public int getStride() {
        return STRIDE;
    }

    private static int encodePosition(float value) {
        return (int) ((MODEL_ORIGIN + value) * (POSITION_MAX_VALUE / MODEL_SCALE));
    }

    private static int encodeBlockLight(int light) {
        return (light >> 0) & 0xFF;
    }

    private static int encodeSkyLight(int light) {
        return (light >> 16) & 0xFF;
    }

    private static int encodeTexture(float value) {
        return (int) (Math.min(0.99999997F, value) * TEXTURE_MAX_VALUE);
    }
}
