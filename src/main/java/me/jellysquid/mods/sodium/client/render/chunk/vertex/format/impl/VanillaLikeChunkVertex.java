package me.jellysquid.mods.sodium.client.render.chunk.vertex.format.impl;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.VanillaLikeChunkMeshAttribute;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorU8;
import org.lwjgl.system.MemoryUtil;

/**
 * This vertex format is less performant and uses more VRAM than {@link CompactChunkVertex}, but should be completely
 * compatible with mods & resource packs that need high precision for models.
 */
public class VanillaLikeChunkVertex implements ChunkVertexType {
    public static final int STRIDE = 24;

    private static final int TEXTURE_MAX_VALUE = 65536;

    public static final GlVertexFormat<VanillaLikeChunkMeshAttribute> VERTEX_FORMAT = GlVertexFormat.builder(VanillaLikeChunkMeshAttribute.class, STRIDE)
            .addElement(VanillaLikeChunkMeshAttribute.POSITION, 0, GlVertexAttributeFormat.FLOAT, 3, false, false)
            .addElement(VanillaLikeChunkMeshAttribute.COLOR, 12, GlVertexAttributeFormat.UNSIGNED_INT, 1, false, true)
            .addElement(VanillaLikeChunkMeshAttribute.TEXTURE_UV, 16, GlVertexAttributeFormat.UNSIGNED_INT, 1, false, true)
            .addElement(VanillaLikeChunkMeshAttribute.DRAW_PARAMS_LIGHT, 20, GlVertexAttributeFormat.UNSIGNED_INT, 1, false, true)
            .build();

    @Override
    public GlVertexFormat<VanillaLikeChunkMeshAttribute> getVertexFormat() {
        return VERTEX_FORMAT;
    }

    @Override
    public ChunkVertexEncoder getEncoder() {
        return (ptr, material, vertex, sectionIndex) -> {
            MemoryUtil.memPutFloat(ptr + 0, vertex.x);
            MemoryUtil.memPutFloat(ptr + 4, vertex.y);
            MemoryUtil.memPutFloat(ptr + 8, vertex.z);
            MemoryUtil.memPutInt(ptr + 12, encodeColor(vertex.color));
            MemoryUtil.memPutInt(ptr + 16, (encodeTexture(vertex.u) << 0) | (encodeTexture(vertex.v) << 16));
            MemoryUtil.memPutInt(ptr + 20, (encodeDrawParameters(material, sectionIndex) << 0) | (encodeLight(vertex.light) << 16));

            return ptr + STRIDE;
        };
    }

    @Override
    public String getDefine() {
        return "VERTEX_FORMAT_FULL";
    }

    private static int encodeDrawParameters(Material material, int sectionIndex) {
        return (((sectionIndex & 0xFF) << 8) | ((material.bits() & 0xFF) << 0));
    }

    private static int encodeColor(int color) {
        var brightness = ColorU8.byteToNormalizedFloat(ColorABGR.unpackAlpha(color));

        int r = ColorU8.normalizedFloatToByte(ColorU8.byteToNormalizedFloat(ColorABGR.unpackRed(color)) * brightness);
        int g = ColorU8.normalizedFloatToByte(ColorU8.byteToNormalizedFloat(ColorABGR.unpackGreen(color)) * brightness);
        int b = ColorU8.normalizedFloatToByte(ColorU8.byteToNormalizedFloat(ColorABGR.unpackBlue(color)) * brightness);

        return ColorABGR.pack(r, g, b, 0x00);
    }

    private static int encodeLight(int light) {
        int block = light & 0xFF;
        int sky = (light >> 16) & 0xFF;
        return ((block << 0) | (sky << 8));
    }

    private static int encodeTexture(float value) {
        return (int) (Math.min(0.99999997F, value) * TEXTURE_MAX_VALUE);
    }
}
