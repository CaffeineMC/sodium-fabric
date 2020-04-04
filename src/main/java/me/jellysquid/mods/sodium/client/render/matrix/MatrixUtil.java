package me.jellysquid.mods.sodium.client.render.matrix;

import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.client.util.math.Matrix4f;

import java.nio.FloatBuffer;

public class MatrixUtil {
    private static final FloatBuffer MATRIX_BUFFER = GlAllocationUtils.allocateFloatBuffer(16);

    /**
     * Writes the {@param matrix} into a temporary {@link FloatBuffer} which can then be used to upload it using
     * an OpenGL function. This is not thread safe and must only called from only one thread at a time.
     */
    public static FloatBuffer writeToBuffer(Matrix4f matrix) {
        FloatBuffer buffer = MATRIX_BUFFER;
        matrix.writeToBuffer(buffer);
        buffer.rewind();

        return buffer;
    }
}
