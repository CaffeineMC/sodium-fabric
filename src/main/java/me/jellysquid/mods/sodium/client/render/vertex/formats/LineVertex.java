package me.jellysquid.mods.sodium.client.render.vertex.formats;

import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatRegistry;
import net.minecraft.client.render.VertexFormats;

import static me.jellysquid.mods.sodium.client.render.vertex.VertexElementSerializer.*;

public final class LineVertex  {
    public static final VertexFormatDescription FORMAT = VertexFormatRegistry.get(VertexFormats.LINES);

    public static final int STRIDE = 20;

    public static void write(long ptr,
                             float x, float y, float z, int color, int normal) {
        setPositionXYZ(ptr + 0, x, y, z);
        setColorABGR(ptr + 12, color);
        setNormalXYZ(ptr + 16, normal);
    }
}
