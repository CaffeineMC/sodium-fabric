package me.jellysquid.mods.sodium.client.render.vertex.formats;

import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatRegistry;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.system.MemoryUtil;

public final class ParticleVertex {
    public static final VertexFormatDescription FORMAT = VertexFormatRegistry.get(VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);

    public static final int STRIDE = 28;

    private static final int OFFSET_POSITION = 0;
    private static final int OFFSET_TEXTURE = 12;
    private static final int OFFSET_COLOR = 20;
    private static final int OFFSET_LIGHT = 24;

    public static void write(long ptr,
                             float x, float y, float z, int color, float u, float v, int light) {
        MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 0, x);
        MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 4, y);
        MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 8, z);

        MemoryUtil.memPutFloat(ptr + OFFSET_TEXTURE + 0, u);
        MemoryUtil.memPutFloat(ptr + OFFSET_TEXTURE + 4, v);

        MemoryUtil.memPutInt(ptr + OFFSET_COLOR, color);

        MemoryUtil.memPutInt(ptr + OFFSET_LIGHT, light);
    }
}
