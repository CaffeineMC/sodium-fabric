package me.jellysquid.mods.sodium.client.render.vertex.formats;

import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatRegistry;
import net.minecraft.client.render.VertexFormats;

import static me.jellysquid.mods.sodium.client.render.vertex.VertexElementSerializer.*;

public final class GlyphVertex  {
    public static final VertexFormatDescription FORMAT = VertexFormatRegistry.get(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT);

    public static final int STRIDE = 28;

    public static void write(long ptr, float x, float y, float z, int color, float u, float v, int light) {
        setPositionXYZ(ptr + 0, x, y, z);
        setColorABGR(ptr + 12, color);
        setTextureUV(ptr + 16, u, v);
        setLightUV(ptr + 24, light);
    }
}
