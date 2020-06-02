package me.jellysquid.mods.sodium.client.render.backends.shader;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.gl.util.GlFogHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.function.Function;

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
public abstract class FogShaderComponent {
    public abstract void setup();

    public static class None extends FogShaderComponent {
        public None(ChunkProgram program) {

        }

        @Override
        public void setup() {

        }
    }

    public static class Exp2 extends FogShaderComponent {
        private final int uFogColor;
        private final int uFogDensity;

        public Exp2(ChunkProgram program) {
            this.uFogColor = program.getUniformLocation("u_FogColor");
            this.uFogDensity = program.getUniformLocation("u_FogDensity");
        }

        @Override
        public void setup() {
            FogShaderComponent.setupColorUniform(this.uFogColor);

            GL20.glUniform1f(this.uFogDensity, GlFogHelper.getFogDensity());
        }
    }

    public static class Linear extends FogShaderComponent {
        private final int uFogColor;
        private final int uFogLength;
        private final int uFogEnd;

        public Linear(ChunkProgram program) {
            this.uFogColor = program.getUniformLocation("u_FogColor");
            this.uFogLength = program.getUniformLocation("u_FogLength");
            this.uFogEnd = program.getUniformLocation("u_FogEnd");
        }

        @Override
        public void setup() {
            FogShaderComponent.setupColorUniform(this.uFogColor);

            float end = GlFogHelper.getFogEnd();
            float start = GlFogHelper.getFogStart();

            GL20.glUniform1f(this.uFogLength, end - start);
            GL20.glUniform1f(this.uFogEnd, end);
        }
    }

    /**
     * Copies the fog color from the deprecated fixed function pipeline and uploads it to the uniform at the
     * given binding index.
     */
    private static void setupColorUniform(int index) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer bufFogColor = stack.mallocFloat(4);
            GL11.glGetFloatv(GL11.GL_FOG_COLOR, bufFogColor);
            GL20.glUniform4fv(index, bufFogColor);
        }
    }

    enum FogMode {
        NONE(None::new, ImmutableList.of()),
        LINEAR(Linear::new, ImmutableList.of("USE_FOG", "USE_FOG_LINEAR")),
        EXP2(Exp2::new, ImmutableList.of("USE_FOG", "USE_FOG_EXP2"));

        private final Function<ChunkProgram, FogShaderComponent> factory;
        private final List<String> defines;

        FogMode(Function<ChunkProgram, FogShaderComponent> factory, List<String> defines) {
            this.factory = factory;
            this.defines = defines;
        }

        public Function<ChunkProgram, FogShaderComponent> getFactory() {
            return this.factory;
        }

        public List<String> getDefines() {
            return this.defines;
        }

        /**
         * Retrieves the current fog mode from the fixed-function pipeline.
         */
        public static FogMode getActiveMode() {
            if (!GL11.glGetBoolean(GL11.GL_FOG)) {
                return FogMode.NONE;
            }

            int mode = GL11.glGetInteger(GL11.GL_FOG_MODE);

            switch (mode) {
                case GL11.GL_EXP2:
                case GL11.GL_EXP:
                    return FogMode.EXP2;
                case GL11.GL_LINEAR:
                    return FogMode.LINEAR;
                default:
                    throw new UnsupportedOperationException("Unknown fog mode: " + mode);
            }
        }
    }
}
