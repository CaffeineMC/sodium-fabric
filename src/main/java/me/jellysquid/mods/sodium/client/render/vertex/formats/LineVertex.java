package me.jellysquid.mods.sodium.client.render.vertex.formats;

import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatRegistry;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.system.MemoryUtil;

public final class LineVertex {
    public static final VertexFormatDescription FORMAT = VertexFormatRegistry.get(VertexFormats.LINES);

    public static final int STRIDE = 20;

    private static final int OFFSET_POSITION = 0;
    private static final int OFFSET_COLOR = 12;
    private static final int OFFSET_NORMAL = 16;

    public static void write(long ptr,
                             float x, float y, float z, int color, int normal) {
        MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 0, x);
        MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 4, y);
        MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 8, z);

        MemoryUtil.memPutInt(ptr + OFFSET_COLOR, color);

        MemoryUtil.memPutInt(ptr + OFFSET_NORMAL, normal);
    }
}
