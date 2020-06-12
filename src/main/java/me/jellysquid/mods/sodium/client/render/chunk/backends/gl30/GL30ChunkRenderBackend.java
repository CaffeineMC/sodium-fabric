package me.jellysquid.mods.sodium.client.render.chunk.backends.gl30;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.func.GlFunctions;
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

import java.util.Iterator;

/**
 * Shader-based render backend for chunks which uses VAOs to avoid the overhead in setting up vertex attribute pointers
 * before every draw call. This approach has significantly less CPU overhead as we only need to cross the native code
 * barrier once in order to setup all the necessary vertex attribute states and buffer bindings. Additionally, it might
 * allow the driver to skip validation logic that would otherwise be performed.
 */
public class GL30ChunkRenderBackend extends ChunkRenderBackendOneshot<VAOGraphicsState> {
    public GL30ChunkRenderBackend(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        super(format);
    }

    @Override
    public void upload(Iterator<ChunkBuildResult<VAOGraphicsState>> queue) {
        while (queue.hasNext()) {
            ChunkBuildResult<VAOGraphicsState> result = queue.next();

            ChunkRenderContainer<VAOGraphicsState> render = result.render;
            ChunkRenderData data = result.data;

            VAOGraphicsState graphics = render.getGraphicsState();
            ChunkMeshData meshData = data.getMeshData();

            if (meshData.hasData()) {
                if (graphics == null) {
                    graphics = new VAOGraphicsState(this.vertexFormat, render.getChunkPos());
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
    public void render(BlockRenderPass pass, Iterator<ChunkRenderContainer<VAOGraphicsState>> renders, MatrixStack matrixStack, ChunkCameraContext camera) {
        super.begin(matrixStack);

        GlVertexArray lastRender = null;

        while (renders.hasNext()) {
            ChunkRenderContainer<VAOGraphicsState> render = renders.next();
            VAOGraphicsState graphics = render.getGraphicsState();

            if (graphics == null) {
                continue;
            }

            long slice = graphics.getSliceForLayer(pass);

            if (VertexSlice.isEmpty(slice)) {
                continue;
            }

            float modelX = camera.getChunkModelOffset(render.getOriginX(), camera.blockOriginX, camera.originX);
            float modelY = camera.getChunkModelOffset(render.getOriginY(), camera.blockOriginY, camera.originY);
            float modelZ = camera.getChunkModelOffset(render.getOriginZ(), camera.blockOriginZ, camera.originZ);

            this.activeProgram.uploadChunkModelOffset(modelX, modelY, modelZ);

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

    public static boolean isSupported() {
        return GlFunctions.isVertexArraySupported();
    }
}
