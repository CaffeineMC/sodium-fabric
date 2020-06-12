package me.jellysquid.mods.sodium.client.render.chunk.backends.gl20;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.util.VertexSlice;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.oneshot.ChunkRenderBackendOneshot;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.util.Iterator;

/**
 * A simple chunk rendering backend which mirrors that of vanilla's own pretty closely.
 */
public class GL20ChunkRenderBackend extends ChunkRenderBackendOneshot<VBOGraphicsState> {
    public GL20ChunkRenderBackend(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        super(format);
    }

    @Override
    public void upload(Iterator<ChunkBuildResult<VBOGraphicsState>> queue) {
        while (queue.hasNext()) {
            ChunkBuildResult<VBOGraphicsState> result = queue.next();

            ChunkRenderContainer<VBOGraphicsState> render = result.render;
            ChunkRenderData data = result.data;

            VBOGraphicsState graphics = render.getGraphicsState();
            ChunkMeshData meshData = data.getMeshData();

            if (meshData.hasData()) {
                if (graphics == null) {
                    graphics = new VBOGraphicsState(this.vertexFormat, render.getChunkPos());
                    render.setGraphicsState(graphics);
                }

                graphics.upload(meshData);
            } else if (graphics != null) {
                graphics.delete();
                render.setGraphicsState(null);
            }

            render.setData(data);
        }
    }

    @Override
    public void render(BlockRenderPass pass, Iterator<ChunkRenderContainer<VBOGraphicsState>> renders, MatrixStack matrixStack, ChunkCameraContext camera) {
        super.begin(matrixStack);

        this.vertexFormat.enableVertexAttributes();

        GlBuffer lastRender = null;

        while (renders.hasNext()) {
            ChunkRenderContainer<VBOGraphicsState> render = renders.next();
            VBOGraphicsState graphics = render.getGraphicsState();

            if (graphics == null) {
                return;
            }

            long slice = graphics.getSliceForLayer(pass);

            if (VertexSlice.isEmpty(slice)) {
                continue;
            }

            float modelX = camera.getChunkModelOffset(render.getOriginX(), camera.blockOriginX, camera.originX);
            float modelY = camera.getChunkModelOffset(render.getOriginY(), camera.blockOriginY, camera.originY);
            float modelZ = camera.getChunkModelOffset(render.getOriginZ(), camera.blockOriginZ, camera.originZ);

            this.activeProgram.uploadChunkModelOffset(modelX, modelY, modelZ);

            GlBuffer buffer = graphics.getBuffer();
            buffer.bind(GL15.GL_ARRAY_BUFFER);

            this.vertexFormat.bindVertexAttributes();

            buffer.drawArrays(GL11.GL_QUADS, VertexSlice.unpackFirst(slice), VertexSlice.unpackCount(slice));

            lastRender = buffer;
        }

        if (lastRender != null) {
            lastRender.unbind(GL15.GL_ARRAY_BUFFER);
        }

        this.vertexFormat.disableVertexAttributes();

        super.end(matrixStack);
    }

    public static boolean isSupported() {
        return true;
    }
}
