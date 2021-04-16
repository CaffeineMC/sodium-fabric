package me.jellysquid.mods.sodium.client.gl.compat;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

/**
 * Deprecated functions used for extracting current matrix state from OpenGL. These are not supported in OpenGL Core.
 */
@Deprecated
public class LegacyMatrixStackHelper {
    /**
     * Obtains a model-view-projection matrix by multiplying the projection matrix in OpenGL state with the model-view
     * matrix from {@param matrices}.
     *
     * The returned buffer is only valid for the lifetime of {@param stack}.
     *
     * @return A float-buffer on the stack containing the model-view-projection matrix in a format suitable for
     * uploading as uniform state
     */
    public static FloatBuffer getModelViewProjectionMatrix(MatrixStack.Entry matrices, MemoryStack stack) {
        FloatBuffer bufProjection = stack.mallocFloat(16);

        // Since vanilla doesn't expose the projection matrix anywhere, we need to grab it from the OpenGL state
        // This isn't super fast, but should be sufficient enough to remain compatible with any state modifying code
        GL20C.glGetFloatv(GL20.GL_PROJECTION_MATRIX, bufProjection);

        FloatBuffer bufModelView = stack.mallocFloat(16);
        FloatBuffer bufModelViewProjection = stack.mallocFloat(16);

        Matrix4f modelMatrix = matrices.getModel();
        modelMatrix.writeToBuffer(bufModelView);

        // Use OpenGL to multiply our matrices together
        GL20.glPushMatrix();
        GL20.glLoadMatrixf(bufProjection);
        GL20.glMultMatrixf(bufModelView);
        GL20.glGetFloatv(GL20.GL_MODELVIEW_MATRIX, bufModelViewProjection);
        GL20.glPopMatrix();

        return bufModelViewProjection;
    }
}
