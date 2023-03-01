package net.caffeinemc.mods.sodium.api.vertex.format.common;

import net.caffeinemc.mods.sodium.api.vertex.attributes.common.*;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatRegistry;
import net.minecraft.client.render.VertexFormats;

public final class ModelVertex {
    public static final VertexFormatDescription FORMAT = VertexFormatRegistry.instance()
            .get(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

    public static final int STRIDE = 36;

    private static final int OFFSET_POSITION = 0;
    private static final int OFFSET_COLOR = 12;
    private static final int OFFSET_TEXTURE = 16;
    private static final int OFFSET_OVERLAY = 24;
    private static final int OFFSET_LIGHT = 28;
    private static final int OFFSET_NORMAL = 32;

    public static void write(long ptr,
                             float x, float y, float z, int color, float u, float v, int overlay, int light, int normal) {
        PositionAttribute.put(ptr + OFFSET_POSITION, x, y, z);
        ColorAttribute.set(ptr + OFFSET_COLOR, color);
        TextureAttribute.put(ptr + OFFSET_TEXTURE, u, v);
        OverlayAttribute.set(ptr + OFFSET_OVERLAY, overlay);
        LightAttribute.set(ptr + OFFSET_LIGHT, light);
        NormalAttribute.set(ptr + OFFSET_NORMAL, normal);
    }
}
