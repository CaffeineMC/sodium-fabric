package me.jellysquid.mods.sodium.client.render.chunk.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.gl.compat.LegacyFogHelper;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

/**
 * These shader implementations try to remain compatible with the deprecated fixed function pipeline by manually
 * copying the state into each shader's uniforms. The shader code itself is a straight-forward implementation of the
 * fog functions themselves from the fixed-function pipeline, except that they use the distance from the camera
 * rather than the z-buffer to produce better looking fog that doesn't move with the player's view angle.
 *
 * Minecraft itself will actually try to enable distance-based fog by using the proprietary NV_fog_distance extension,
 * but as the name implies, this only works on graphics cards produced by NVIDIA. The shader implementation however does
 * not depend on any vendor-specific extensions and is written using very simple GLSL code.
 */
public abstract class ChunkShaderFogComponent {
    public abstract void setup();

    public static class None extends ChunkShaderFogComponent {
        public None(ChunkProgram program) {

        }

        @Override
        public void setup() {

        }
    }

    public static class Smooth extends ChunkShaderFogComponent {
        private final int uFogColor;
        private final int uFogStart;
        private final int uFogEnd;

        public Smooth(ChunkProgram program) {
            this.uFogColor = program.getUniformLocation("u_FogColor");
            this.uFogStart = program.getUniformLocation("u_FogStart");
            this.uFogEnd = program.getUniformLocation("u_FogEnd");
        }

        @Override
        public void setup() {
            ChunkShaderFogComponent.setupColorUniform(this.uFogColor);

            float end = 0.0f;
            float start = 128.0f;

            GL20C.glUniform1f(this.uFogStart, start);
            GL20C.glUniform1f(this.uFogEnd, end);
        }
    }

    /**
     * Copies the fog color from the deprecated fixed function pipeline and uploads it to the uniform at the
     * given binding index.
     */
    private static void setupColorUniform(int index) {
//        GL20C.glUniform4fv(index, RenderSystem.getShaderFogColor());
    }

}
