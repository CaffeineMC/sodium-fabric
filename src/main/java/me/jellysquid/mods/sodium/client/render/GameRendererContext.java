package me.jellysquid.mods.sodium.client.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class GameRendererContext {
    private static Matrix4f PROJECTION_MATRIX;

    public static void captureProjectionMatrix(Matrix4f matrix) {
        PROJECTION_MATRIX = matrix.copy();
    }

    /**
     * Obtains a model-view-projection matrix by multiplying the projection matrix with the model-view matrix
     * from {@param matrices}.
     *
     * The returned buffer is only valid for the lifetime of {@param stack}.
     *
     * @return A float-buffer on the stack containing the model-view-projection matrix in a format suitable for
     * uploading as uniform state
     */
    public static FloatBuffer getModelViewProjectionMatrix(MatrixStack.Entry matrices, MemoryStack memoryStack) {
        if (PROJECTION_MATRIX == null) {
            throw new IllegalStateException("Projection matrix has not been captured");
        }

        FloatBuffer bufModelViewProjection = memoryStack.mallocFloat(16);

        Matrix4f matrix = PROJECTION_MATRIX.copy();
        matrix.multiply(matrices.getModel());
        matrix.writeToBuffer(bufModelViewProjection);

        return bufModelViewProjection;
    }
}
