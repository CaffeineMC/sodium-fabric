package me.jellysquid.mods.sodium.client.render.vertex.formats;

import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatRegistry;
import net.minecraft.client.render.VertexFormats;

import static me.jellysquid.mods.sodium.client.render.vertex.VertexElementSerializer.*;

public final class ParticleVertex {
    public static final VertexFormatDescription FORMAT = VertexFormatRegistry.get(VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);

    public static final int STRIDE = 28;

    public static void write(long ptr,
                             float x, float y, float z, float u, float v, int color, int light) {
        setPositionXYZ(ptr + 0, x, y, z);
        setTextureUV(ptr + 12, u, v);
        setColorABGR(ptr + 20, color);
        setLightUV(ptr + 24, light);
    }
}
