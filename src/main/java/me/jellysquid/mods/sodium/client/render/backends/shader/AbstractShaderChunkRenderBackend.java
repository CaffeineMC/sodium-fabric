package me.jellysquid.mods.sodium.client.render.backends.shader;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlImmutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.gl.shader.GlShaderProgram;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderType;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.render.backends.AbstractChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkMesh;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRender;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderData;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL15;

import java.util.Iterator;

public abstract class AbstractShaderChunkRenderBackend<T extends ChunkRenderState> extends AbstractChunkRenderBackend<T> {
    protected final ChunkShader program;
    protected final boolean useImmutableStorage;

    public AbstractShaderChunkRenderBackend() {
        SodiumGameOptions options = SodiumClientMod.options();

        this.useImmutableStorage = GlImmutableBuffer.isSupported() && options.performance.useImmutableStorage;

        GlShader vertShader = ShaderLoader.loadShader(ShaderType.VERTEX, new Identifier("sodium", "chunk.v.glsl"));
        GlShader fragShader = ShaderLoader.loadShader(ShaderType.FRAGMENT, new Identifier("sodium", "chunk.f.glsl"));

        try {
            this.program = GlShaderProgram.builder(new Identifier("sodium", "chunk_shader"))
                    .attach(vertShader)
                    .attach(fragShader)
                    .link(ChunkShader::new);
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    @Override
    public void upload(Iterator<ChunkBuildResult<T>> queue) {
        GlBuffer lastBuffer = null;

        while (queue.hasNext()) {
            ChunkBuildResult<T> result = queue.next();

            ChunkRender<T> render = result.render;
            ChunkRenderData data = result.data;

            render.resetRenderStates();
            render.setData(data);

            for (ChunkMesh mesh : data.getMeshes()) {
                GlBuffer buffer = this.createBuffer(GL15.GL_ARRAY_BUFFER);
                buffer.bind();
                buffer.upload(mesh.takePendingUpload());

                lastBuffer = buffer;

                render.setRenderState(mesh.getRenderPass(), this.createRenderState(buffer, render));
            }
        }

        if (lastBuffer != null) {
            lastBuffer.unbind();
        }
    }

    private GlBuffer createBuffer(int target) {
        return this.useImmutableStorage ? new GlImmutableBuffer(target) : new GlMutableBuffer(target);
    }

    @Override
    public void begin(MatrixStack matrixStack) {
        super.begin(matrixStack);

        this.program.bind();
    }

    @Override
    public void end(MatrixStack matrixStack) {
        this.program.unbind();

        super.end(matrixStack);
    }

    @Override
    public void delete() {
        this.program.delete();
    }

    protected abstract T createRenderState(GlBuffer buffer, ChunkRender<T> render);
}
