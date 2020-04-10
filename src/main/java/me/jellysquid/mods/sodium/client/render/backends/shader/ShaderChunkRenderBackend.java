package me.jellysquid.mods.sodium.client.render.backends.shader;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.buffer.GlImmutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlTessellation;
import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.gl.shader.GlShaderProgram;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderType;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.render.backends.AbstractChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRender;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class ShaderChunkRenderBackend extends AbstractChunkRenderBackend<ShaderChunkRenderState> {
    private final ChunkShader program;

    private final boolean useVertexArrays;
    private final boolean useImmutableStorage;

    private GlTessellation lastRender;

    public ShaderChunkRenderBackend() {
        SodiumGameOptions options = SodiumClientMod.options();

        this.useVertexArrays = GlVertexArray.isSupported() && options.performance.useVertexArrays;
        this.useImmutableStorage = GlImmutableBuffer.isSupported() && options.performance.useImmutableStorage;

        GlShader vertShader = new GlShader(ShaderType.VERTEX, ShaderLoader.getShaderSource("/assets/sodium/shaders/chunk.v.glsl"));
        GlShader fragShader = new GlShader(ShaderType.FRAGMENT, ShaderLoader.getShaderSource("/assets/sodium/shaders/chunk.f.glsl"));

        try {
            this.program = GlShaderProgram.builder()
                    .attach(vertShader)
                    .attach(fragShader)
                    .link(ChunkShader::new);
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    @Override
    public ShaderChunkRenderState createRenderState() {
        return new ShaderChunkRenderState(this.useVertexArrays, this.useImmutableStorage);
    }

    @Override
    public void begin(MatrixStack matrixStack) {
        super.begin(matrixStack);

        this.program.bind();
    }

    @Override
    public void render(ChunkRender<ShaderChunkRenderState> chunk, BlockRenderPass layer, MatrixStack matrixStack, double x, double y, double z) {
        ShaderChunkRenderState data = chunk.getRenderState();
        GlTessellation tess = data.getVertexArrayForLayer(layer);

        if (tess == null) {
            return;
        }

        this.program.uploadModelMatrix(this.createModelMatrix(chunk, x, y, z));

        tess.bind();
        tess.draw(GL11.GL_QUADS);

        this.lastRender = tess;
    }

    @Override
    public void end(MatrixStack matrixStack) {
        super.end(matrixStack);

        if (this.lastRender != null) {
            this.lastRender.unbind();
            this.lastRender = null;
        }

        this.program.unbind();
    }

    @Override
    public void delete() {
        this.program.delete();
    }

    private static class ChunkShader extends GlShaderProgram {
        private final int uModelView;
        private final int uProjection;
        private final int uBlockTex;
        private final int uLightTex;

        public ChunkShader(int handle) {
            super(handle);

            this.uModelView = this.getUniformLocation("u_ModelView");
            this.uProjection = this.getUniformLocation("u_Projection");

            this.uBlockTex = this.getUniformLocation("u_BlockTex");
            this.uLightTex = this.getUniformLocation("u_LightTex");
        }

        @Override
        public void bind() {
            super.bind();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buf = stack.mallocFloat(16);

                GL15.glGetFloatv(GL15.GL_PROJECTION_MATRIX, buf);
                GL21.glUniformMatrix4fv(this.uProjection, false, buf);
            }

            GL21.glUniform1i(this.uBlockTex, 0);
            GL21.glUniform1i(this.uLightTex, 2);
        }

        public void uploadModelMatrix(FloatBuffer buffer) {
            GL21.glUniformMatrix4fv(this.uModelView, false, buffer);
        }
    }
}
