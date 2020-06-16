package me.jellysquid.mods.sodium.client.render.chunk.backends.gl20;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
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
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.util.Iterator;

/**
 * A simple chunk rendering backend which mirrors that of vanilla's own pretty closely.
 */
public class GL20ChunkRenderBackend extends ChunkRenderBackendOneshot<VBOGraphicsState> {
    private final GlMultiDrawBatch batch = new GlMultiDrawBatch(ChunkModelPart.count());

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
                    graphics = new VBOGraphicsState();
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
        GlMultiDrawBatch batch = this.batch;

        while (renders.hasNext()) {
            ChunkRenderContainer<VBOGraphicsState> render = renders.next();
            VBOGraphicsState graphics = render.getGraphicsState();

            if (graphics == null || !graphics.containsDataForPass(pass)) {
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

                    GlBuffer buffer = graphics.getBuffer();
                    buffer.bind(GL15.GL_ARRAY_BUFFER);

                    this.vertexFormat.bindVertexAttributes();

                    lastRender = buffer;
                }

                batch.addChunkRender(part.start, part.count);
            }

            if (batch.isBuilding()) {
                batch.end();

                GL20.glMultiDrawArrays(GL11.GL_QUADS, this.batch.getIndicesBuffer(), this.batch.getLengthBuffer());
            }
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
