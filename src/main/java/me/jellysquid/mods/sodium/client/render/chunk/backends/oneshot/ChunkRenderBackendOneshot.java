package me.jellysquid.mods.sodium.client.render.chunk.backends.oneshot;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.DrawCommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.gl.util.GlMultiDrawBatch;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterator;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkRenderShaderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;
import net.minecraft.client.render.VertexFormat;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.Iterator;

public class ChunkRenderBackendOneshot extends ChunkRenderShaderBackend<ChunkOneshotGraphicsState> {
    private final GlMultiDrawBatch batch = new GlMultiDrawBatch(ModelQuadFacing.COUNT);

    public ChunkRenderBackendOneshot(ChunkVertexType vertexType) {
        super(vertexType);
    }

    @Override
    public void upload(CommandList commandList, Iterator<ChunkBuildResult<ChunkOneshotGraphicsState>> queue) {
        while (queue.hasNext()) {
            ChunkBuildResult<ChunkOneshotGraphicsState> result = queue.next();

            ChunkRenderContainer<ChunkOneshotGraphicsState> render = result.render;
            ChunkRenderData data = result.data;

            for (BlockRenderPass pass : BlockRenderPass.VALUES) {
                ChunkOneshotGraphicsState state = render.getGraphicsState(pass);
                ChunkMeshData mesh = data.getMesh(pass);

                if (mesh.hasVertexData()) {
                    if (state == null) {
                        state = new ChunkOneshotGraphicsState(RenderDevice.INSTANCE, render);
                    }

                    state.upload(commandList, mesh);
                } else {
                    if (state != null) {
                        state.delete(commandList);
                    }

                    state = null;
                }

                render.setGraphicsState(pass, state);
            }

            render.setData(data);
        }
    }

    @Override
    public void render(CommandList commandList, ChunkRenderListIterator<ChunkOneshotGraphicsState> it, ChunkCameraContext camera) {
        while (it.hasNext()) {
            ChunkOneshotGraphicsState state = it.getGraphicsState();
            int visibleFaces = it.getVisibleFaces();

            this.buildBatch(state, visibleFaces);

            if (this.batch.isBuilding()) {
                this.prepareDrawBatch(camera, state);
                this.drawBatch(commandList, state);
            }

            it.advance();
        }
    }

    protected void prepareDrawBatch(ChunkCameraContext camera, ChunkOneshotGraphicsState state) {
        float modelX = camera.getChunkModelOffset(state.getX(), camera.blockOriginX, camera.originX);
        float modelY = camera.getChunkModelOffset(state.getY(), camera.blockOriginY, camera.originY);
        float modelZ = camera.getChunkModelOffset(state.getZ(), camera.blockOriginZ, camera.originZ);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(4);
            fb.put(0, modelX);
            fb.put(1, modelY);
            fb.put(2, modelZ);

            GL20C.glVertexAttrib4fv(ChunkShaderBindingPoints.MODEL_OFFSET.getGenericAttributeIndex(), fb);
        }
    }

    protected void buildBatch(ChunkOneshotGraphicsState state, int visibleFaces) {
        GlMultiDrawBatch batch = this.batch;
        batch.begin();

        for (int i = 0; i < ModelQuadFacing.COUNT; i++) {
            if ((visibleFaces & (1 << i)) == 0) {
                continue;
            }

            long part = state.getModelPart(i);
            // FIXME: is this the best location to add the * 6 / 4?
            batch.addChunkRender(BufferSlice.unpackStart(part), BufferSlice.unpackLength(part) * 6 / 4);
        }
    }

    protected void drawBatch(CommandList commandList, ChunkOneshotGraphicsState state) {
        this.batch.end();


        RenderSystem.IndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS, 24_000);

        try (DrawCommandList drawCommandList = commandList.beginTessellating(state.tessellation)) {
            drawCommandList.multiDrawElementArrays(this.batch.getIndicesBuffer(), indexBuffer, this.batch.getLengthBuffer());
        }
    }

    @Override
    public void delete() {
        super.delete();

        this.batch.delete();
    }

    @Override
    public Class<ChunkOneshotGraphicsState> getGraphicsStateType() {
        return ChunkOneshotGraphicsState.class;
    }

    @Override
    public String getRendererName() {
        return "Oneshot";
    }
}
