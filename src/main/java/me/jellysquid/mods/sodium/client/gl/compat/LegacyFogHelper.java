package me.jellysquid.mods.sodium.client.gl.compat;

import com.mojang.blaze3d.systems.RenderSystem;
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
    /**
     * Retrieves the current fog mode from the fixed-function pipeline.
     */
    public static ChunkFogMode getFogMode() {
        return ChunkFogMode.NONE;
    }

    public static float getFogCutoff() {
        return 128.0f;
//        return RenderSystem.getShaderFogEnd();
    }
}
