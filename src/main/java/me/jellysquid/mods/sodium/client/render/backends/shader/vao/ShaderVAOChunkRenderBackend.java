package me.jellysquid.mods.sodium.client.render.backends.shader.vao;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
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

import java.util.Iterator;

/**
 * Shader-based render backend for chunks which uses VAOs to avoid the overhead in setting up vertex attribute pointers
 * before every draw call. This approach has significantly less CPU overhead as we only need to cross the native code
 * barrier once in order to setup all the necessary vertex attribute states and buffer bindings. Additionally, it might
 * allow the driver to skip validation logic that would otherwise be performed.
 */
public class ShaderVAOChunkRenderBackend extends AbstractShaderChunkRenderBackend<ShaderVAOGraphicsState> {
    public ShaderVAOChunkRenderBackend(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        super(format);
    }

    @Override
    public void upload(Iterator<ChunkBuildResult<ShaderVAOGraphicsState>> queue) {
        while (queue.hasNext()) {
            ChunkBuildResult<ShaderVAOGraphicsState> result = queue.next();

            ChunkRenderContainer<ShaderVAOGraphicsState> render = result.render;
            ChunkRenderData data = result.data;

            ShaderVAOGraphicsState graphics = render.getGraphicsState();
            ChunkMeshData meshData = data.getMeshData();

            if (meshData.hasData()) {
                if (graphics == null) {
                    graphics = new ShaderVAOGraphicsState(this.vertexFormat, render.getChunkPos());
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
    public void render(BlockRenderPass pass, Iterator<ChunkRenderContainer<ShaderVAOGraphicsState>> renders, MatrixStack matrixStack, double x, double y, double z) {
        this.begin(matrixStack);

        int chunkX = (int) (x / 16.0D);
        int chunkY = (int) (y / 16.0D);
        int chunkZ = (int) (z / 16.0D);

        this.activeProgram.setupModelViewMatrix(matrixStack, x % 16.0D, y % 16.0D, z % 16.0D);

        GlVertexArray lastRender = null;

        while (renders.hasNext()) {
            ChunkRenderContainer<ShaderVAOGraphicsState> render = renders.next();
            ShaderVAOGraphicsState graphics = render.getGraphicsState();

            if (graphics == null) {
                continue;
            }

            long slice = graphics.getSliceForLayer(pass);

            if (VertexSlice.isEmpty(slice)) {
                continue;
            }

            this.activeProgram.setupChunk(graphics.getOrigin(), chunkX, chunkY, chunkZ);

            GlVertexArray vao = graphics.getVertexArray();
            vao.bind();

            GlBuffer vbo = graphics.getVertexBuffer();
            vbo.drawArrays(GL11.GL_QUADS, VertexSlice.unpackFirst(slice), VertexSlice.unpackCount(slice));

            lastRender = vao;
        }

        if (lastRender != null) {
            lastRender.unbind();
        }

        this.end(matrixStack);
    }
}
