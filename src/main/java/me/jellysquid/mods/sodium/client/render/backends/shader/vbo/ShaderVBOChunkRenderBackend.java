package me.jellysquid.mods.sodium.client.render.backends.shader.vbo;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.util.VertexSlice;
import me.jellysquid.mods.sodium.client.render.backends.shader.AbstractShaderChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.util.Iterator;

/**
 * A simple chunk rendering backend which mirrors that of vanilla's own pretty closely.
 */
public class ShaderVBOChunkRenderBackend extends AbstractShaderChunkRenderBackend<ShaderVBOGraphicsState> {
    public ShaderVBOChunkRenderBackend(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        super(format);
    }

    @Override
    public void upload(Iterator<ChunkBuildResult<ShaderVBOGraphicsState>> queue) {
        while (queue.hasNext()) {
            ChunkBuildResult<ShaderVBOGraphicsState> result = queue.next();

            ChunkRenderContainer<ShaderVBOGraphicsState> render = result.render;
            ChunkRenderData data = result.data;

            ShaderVBOGraphicsState graphics = render.getGraphicsState();
            ChunkMeshData meshData = data.getMeshData();

            if (meshData.hasData()) {
                if (graphics == null) {
                    graphics = new ShaderVBOGraphicsState(this.vertexFormat, render.getChunkPos());
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
    public void render(BlockRenderPass pass, Iterator<ChunkRenderContainer<ShaderVBOGraphicsState>> renders, MatrixStack matrixStack, double x, double y, double z) {
        super.begin(matrixStack);

        this.vertexFormat.enableVertexAttributes();

        int chunkX = (int) (x / 16.0D);
        int chunkY = (int) (y / 16.0D);
        int chunkZ = (int) (z / 16.0D);

        this.activeProgram.setupModelViewMatrix(matrixStack, x % 16.0D, y % 16.0D, z % 16.0D);

        GlBuffer lastRender = null;

        while (renders.hasNext()) {
            ChunkRenderContainer<ShaderVBOGraphicsState> render = renders.next();
            ShaderVBOGraphicsState graphics = render.getGraphicsState();

            if (graphics == null) {
                return;
            }

            long slice = graphics.getSliceForLayer(pass);

            if (VertexSlice.isEmpty(slice)) {
                continue;
            }

            this.activeProgram.setupChunk(graphics.getOrigin(), chunkX, chunkY, chunkZ);

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
}
