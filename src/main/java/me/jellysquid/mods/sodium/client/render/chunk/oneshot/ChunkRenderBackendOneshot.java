package me.jellysquid.mods.sodium.client.render.chunk.oneshot;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderType;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.gl.util.GlMultiDrawBatch;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkFogMode;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkRenderShaderBackend;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.Iterator;

public abstract class ChunkRenderBackendOneshot<T extends ChunkOneshotGraphicsState> extends ChunkRenderShaderBackend<T, ChunkProgramOneshot> {
    private final GlMultiDrawBatch batch = new GlMultiDrawBatch(ModelQuadFacing.COUNT);

    public ChunkRenderBackendOneshot(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        super(format);
    }

    @Override
    protected ChunkProgramOneshot createShaderProgram(Identifier name, int handle, ChunkFogMode fogMode) {
        return new ChunkProgramOneshot(name, handle, fogMode.getFactory());
    }

    @Override
    protected GlShader createVertexShader(ChunkFogMode fogMode) {
        return ShaderLoader.loadShader(ShaderType.VERTEX, new Identifier("sodium", "chunk_gl20.v.glsl"), fogMode.getDefines());
    }

    @Override
    protected GlShader createFragmentShader(ChunkFogMode fogMode) {
        return ShaderLoader.loadShader(ShaderType.FRAGMENT, new Identifier("sodium", "chunk_gl20.f.glsl"), fogMode.getDefines());
    }

    @Override
    public void upload(Iterator<ChunkBuildResult<T>> queue) {
        while (queue.hasNext()) {
            ChunkBuildResult<T> result = queue.next();

            ChunkRenderContainer<T> render = result.render;
            ChunkRenderData data = result.data;

            for (BlockRenderPass pass : BlockRenderPass.VALUES) {
                T state = render.getGraphicsState(pass);
                ChunkMeshData mesh = data.getMesh(pass);

                if (mesh.hasVertexData()) {
                    if (state == null) {
                        state = this.createGraphicsState();
                    }

                    state.upload(mesh);
                } else {
                    if (state != null) {
                        state.delete();
                    }

                    state = null;
                }

                render.setGraphicsState(pass, state);
            }

            render.setData(data);
        }
    }

    @Override
    public void render(BlockRenderPass pass, Iterator<ChunkRenderContainer<T>> renders, MatrixStack matrixStack, ChunkCameraContext camera) {
        while (renders.hasNext()) {
            ChunkRenderContainer<T> render = renders.next();
            T state = render.getGraphicsState(pass);

            if (state != null) {
                this.buildBatch(render, state);

                if (!this.batch.isEmpty()) {
                    this.prepareDraw(camera, render);
                    this.drawBatch(state);
                }
            }
        }
    }

    protected void prepareDraw(ChunkCameraContext camera, ChunkRenderContainer<T> render) {
        float modelX = camera.getChunkModelOffset(render.getRenderX(), camera.blockOriginX, camera.originX);
        float modelY = camera.getChunkModelOffset(render.getRenderY(), camera.blockOriginY, camera.originY);
        float modelZ = camera.getChunkModelOffset(render.getRenderZ(), camera.blockOriginZ, camera.originZ);

        this.activeProgram.setModelOffset(modelX, modelY, modelZ);
    }

    protected void buildBatch(ChunkRenderContainer<?> render, T state) {
        GlMultiDrawBatch batch = this.batch;
        batch.begin();

        for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
            if (render.isFaceVisible(facing)) {
                BufferSlice part = state.getModelPart(facing);

                if (part != null) {
                    batch.addChunkRender(part.start, part.len);
                }
            }
        }

        batch.end();
    }

    protected void drawBatch(T state) {
        state.bind();

        GL20.glMultiDrawArrays(GL11.GL_QUADS, this.batch.getIndicesBuffer(), this.batch.getLengthBuffer());
    }

    protected abstract T createGraphicsState();
}
