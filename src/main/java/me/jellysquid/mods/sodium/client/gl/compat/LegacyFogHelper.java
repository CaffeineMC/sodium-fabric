package me.jellysquid.mods.sodium.client.gl.compat;

import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkFogMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

/**
 * Deprecated functions used for extracting the current fog parameters from OpenGL state, only relevant for the
 * fixed-function pipeline. These are not supported in OpenGL Core.
 */
@Deprecated
public class LegacyFogHelper {
    private static final float FAR_PLANE_THRESHOLD_EXP = (float) Math.log(1.0f / 0.0019f);
    private static final float FAR_PLANE_THRESHOLD_EXP2 = MathHelper.sqrt(FAR_PLANE_THRESHOLD_EXP);

    public static float getFogEnd() {
        return GL20C.glGetFloat(GL20.GL_FOG_END);
    }

    public static float getFogStart() {
        return GL20C.glGetFloat(GL20.GL_FOG_START);
    }

    public static float getFogDensity() {
        return GL20C.glGetFloat(GL20.GL_FOG_DENSITY);
    }

    /**
     * Retrieves the current fog mode from the fixed-function pipeline.
     */
    public static ChunkFogMode getFogMode() {
        if (!GL20C.glGetBoolean(GL20.GL_FOG)) {
            return ChunkFogMode.NONE;
        }

        int mode = GL20C.glGetInteger(GL20.GL_FOG_MODE);

        switch (mode) {
            case GL20.GL_EXP2:
            case GL20.GL_EXP:
                return ChunkFogMode.EXP2;
            case GL20.GL_LINEAR:
                return ChunkFogMode.LINEAR;
            default:
                throw new UnsupportedOperationException("Unknown fog mode: " + mode);
        }
    }

    public static float getFogCutoff() {
        int mode = GL20C.glGetInteger(GL20.GL_FOG_MODE);

        switch (mode) {
            case GL20.GL_LINEAR:
                return getFogEnd();
            case GL20.GL_EXP:
                return FAR_PLANE_THRESHOLD_EXP / getFogDensity();
            case GL20.GL_EXP2:
                return FAR_PLANE_THRESHOLD_EXP2 / getFogDensity();
            default:
                return 0.0f;
        }
    }

    public static void getFogColor(FloatBuffer buf) {
        GL20C.glGetFloatv(GL20.GL_FOG_COLOR, buf);
    }
}
