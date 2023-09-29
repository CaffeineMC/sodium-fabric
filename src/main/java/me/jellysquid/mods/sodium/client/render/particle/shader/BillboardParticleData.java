package me.jellysquid.mods.sodium.client.render.particle.shader;

import net.caffeinemc.mods.sodium.api.vertex.attributes.common.ColorAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.LightAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.PositionAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.TextureAttribute;
import org.lwjgl.system.MemoryUtil;

public class BillboardParticleData {
    public static final int POSITION_OFFSET = 0;
    public static final int SIZE_OFFSET = 12;
    public static final int COLOR_OFFSET = 16;
    public static final int LIGHT_UV_OFFSET = 20;
    public static final int ANGLE_OFFSET = 24;
    public static final int TEXTURE_INDEX_OFFSET = 28;
    public static final int STRIDE = 32;

    public static void put(long ptr, float x, float y, float z, int color,
                           int light, float size, float angle, int textureIndex
    ) {
        PositionAttribute.put(ptr + POSITION_OFFSET, x, y, z);
        MemoryUtil.memPutFloat(ptr + SIZE_OFFSET, size);
        ColorAttribute.set(ptr + COLOR_OFFSET, color);
        LightAttribute.set(ptr + LIGHT_UV_OFFSET, light);
        MemoryUtil.memPutFloat(ptr + ANGLE_OFFSET, angle);
        MemoryUtil.memPutInt(ptr + TEXTURE_INDEX_OFFSET, textureIndex);
    }
}
