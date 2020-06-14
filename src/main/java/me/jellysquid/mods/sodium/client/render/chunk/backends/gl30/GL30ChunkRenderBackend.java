package me.jellysquid.mods.sodium.client.render.chunk.backends.gl30;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.func.GlFunctions;
import me.jellysquid.mods.sodium.client.gl.util.GlMultiDrawBatch;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkModelPart;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.oneshot.ChunkRenderBackendOneshot;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.Iterator;

/**
 * Shader-based render backend for chunks which uses VAOs to avoid the overhead in setting up vertex attribute pointers
 * before every draw call. This approach has significantly less CPU overhead as we only need to cross the native code
 * barrier once in order to setup all the necessary vertex attribute states and buffer bindings. Additionally, it might
 * allow the driver to skip validation logic that would otherwise be performed.
 */
public class GL30ChunkRenderBackend extends ChunkRenderBackendOneshot<VAOGraphicsState> {
    private final GlMultiDrawBatch batch = new GlMultiDrawBatch(ChunkModelPart.count());

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
                    render.setGraphicsState(graphics = new VAOGraphicsState());
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
        GlMultiDrawBatch batch = this.batch;

        while (renders.hasNext()) {
            ChunkRenderContainer<VAOGraphicsState> render = renders.next();
            VAOGraphicsState graphics = render.getGraphicsState();

            if (graphics == null) {
                continue;
            }

            float modelX = camera.getChunkModelOffset(render.getRenderX(), camera.blockOriginX, camera.originX);
            float modelY = camera.getChunkModelOffset(render.getRenderY(), camera.blockOriginY, camera.originY);
            float modelZ = camera.getChunkModelOffset(render.getRenderZ(), camera.blockOriginZ, camera.originZ);

            for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
                if (!render.isFaceVisible(facing)) {
                    continue;
                }

                ChunkModelPart part = graphics.getModelPart(ChunkModelPart.encodeKey(pass, facing));

                if (part == null) {
                    continue;
                }

                if (!batch.isBuilding()) {
                    batch.begin();

                    this.activeProgram.uploadChunkModelOffset(modelX, modelY, modelZ);

                    GlVertexArray vao = graphics.getVertexArray();
                    vao.bind();

                    lastRender = vao;
                }

                batch.addChunkRender(part.start, part.count);
            }

            if (batch.isBuilding()) {
                batch.end();

                GL20.glMultiDrawArrays(GL11.GL_QUADS, this.batch.getIndicesBuffer(), this.batch.getLengthBuffer());
            }
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
