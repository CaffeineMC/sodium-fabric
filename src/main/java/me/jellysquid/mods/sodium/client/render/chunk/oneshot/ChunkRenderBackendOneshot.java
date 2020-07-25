package me.jellysquid.mods.sodium.client.render.chunk.oneshot;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.gl.util.GlMultiDrawBatch;
import me.jellysquid.mods.sodium.client.gl.util.MemoryTracker;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterator;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkRenderShaderBackend;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.Iterator;

public abstract class ChunkRenderBackendOneshot<T extends ChunkOneshotGraphicsState> extends
        ChunkRenderShaderBackend<T, ChunkProgramOneshot> {
    private final GlMultiDrawBatch batch = new GlMultiDrawBatch(ModelQuadFacing.COUNT);
    private final MemoryTracker memoryTracker = new MemoryTracker();

    public ChunkRenderBackendOneshot(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        super(format);
    }

    @Override
    public void uploadChunks(Iterator<ChunkBuildResult<T>> queue) {
        while (queue.hasNext()) {
            ChunkBuildResult<T> result = queue.next();

            ChunkRenderContainer<T> render = result.render;
            ChunkRenderData data = result.data;

            for (BlockRenderPass pass : this.getRenderPassManager().getSortedPasses()) {
                T state = render.getGraphicsState(pass);
                ChunkMeshData mesh = data.getMesh(pass);

                if (mesh != null) {
                    if (state == null) {
                        state = this.createGraphicsState(this.memoryTracker, render);
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
    public void renderChunks(MatrixStack matrixStack, BlockRenderPass pass, ChunkRenderListIterator<T> it, ChunkCameraContext camera) {
        this.beginRender(matrixStack, pass);

        while (it.hasNext()) {
            T state = it.getGraphicsState();
            int visibleFaces = it.getVisibleFaces();

            this.buildBatch(state, visibleFaces);

            if (this.batch.isBuilding()) {
                this.batch.end();

                this.prepareDrawBatch(camera, state);
                this.drawBatch();
            }

            it.advance();
        }

        this.endRender(matrixStack);
    }

    protected void prepareDrawBatch(ChunkCameraContext camera, T state) {
        float modelX = camera.getChunkModelOffset(state.getX(), camera.blockOriginX, camera.originX);
        float modelY = camera.getChunkModelOffset(state.getY(), camera.blockOriginY, camera.originY);
        float modelZ = camera.getChunkModelOffset(state.getZ(), camera.blockOriginZ, camera.originZ);

        this.activeProgram.setModelOffset(modelX, modelY, modelZ);

        state.bind();
    }

    protected void buildBatch(T state, int visibleFaces) {
        GlMultiDrawBatch batch = this.batch;
        batch.begin();

        for (int i = 0; i < ModelQuadFacing.COUNT; i++) {
            if ((visibleFaces & (1 << i)) == 0) {
                continue;
            }

            long part = state.getModelPart(i);
            batch.addChunkRender(BufferSlice.unpackStart(part), BufferSlice.unpackLength(part));
        }
    }

    protected void drawBatch() {
        GL20.glMultiDrawArrays(GL11.GL_QUADS, this.batch.getIndicesBuffer(), this.batch.getLengthBuffer());
    }

    protected abstract T createGraphicsState(MemoryTracker memoryTracker, ChunkRenderContainer<T> container);

    @Override
    public void delete() {
        super.delete();

        this.batch.delete();
    }

    @Override
    public MemoryTracker getMemoryTracker() {
        return this.memoryTracker;
    }
}
