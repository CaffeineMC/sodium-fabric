package me.jellysquid.mods.sodium.client.render.vertex.formats;

import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatRegistry;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.common.util.MatrixHelper;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

import static me.jellysquid.mods.sodium.client.render.vertex.VertexElementSerializer.*;

public final class ColorVertex {
    public static final VertexFormatDescription FORMAT = VertexFormatRegistry.get(VertexFormats.POSITION_COLOR);

    public static final int STRIDE = 16;

    public static void write(long ptr, Matrix4f matrix, float x, float y, float z, int color) {
        float x2 = MatrixHelper.transformPositionX(matrix, x, y, z);
        float y2 = MatrixHelper.transformPositionY(matrix, x, y, z);
        float z2 = MatrixHelper.transformPositionZ(matrix, x, y, z);

        write(ptr, x2, y2, z2, color);
    }

    public static void write(long ptr, float x, float y, float z, int color) {
        setPositionXYZ(ptr + 0, x, y, z);
        setColorABGR(ptr + 12, color);
    }
}
