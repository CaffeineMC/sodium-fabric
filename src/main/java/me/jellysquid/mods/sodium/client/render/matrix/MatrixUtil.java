package me.jellysquid.mods.sodium.client.render.matrix;

import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.client.util.math.Matrix4f;

import java.nio.FloatBuffer;

public class MatrixUtil {
    private static final FloatBuffer MATRIX_BUFFER = GlAllocationUtils.allocateFloatBuffer(16);

    public static FloatBuffer intoBuffer(Matrix4f matrix) {
        FloatBuffer buffer = MATRIX_BUFFER;
        matrix.writeToBuffer(buffer);
        buffer.rewind();

        return buffer;
    }
}
