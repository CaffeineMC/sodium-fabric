package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkRender;
import me.jellysquid.mods.sodium.client.render.gl.GlVertexArrayBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;

public class ChunkRendererVAO extends AbstractChunkRenderer<ChunkRenderDataVAO> {
    private GlVertexArrayBuffer lastRender;

    @Override
    public ChunkRenderDataVAO createRenderData() {
        return new ChunkRenderDataVAO();
    }

    @Override
    public void render(ChunkRender<ChunkRenderDataVAO> chunk, RenderLayer layer, MatrixStack matrixStack, double x, double y, double z) {
        ChunkRenderDataVAO data = chunk.getRenderData();
        GlVertexArrayBuffer vao = data.getVertexArrayForLayer(layer);

        if (vao == null) {
            return;
        }

        this.beginChunkRender(chunk, x, y, z);

        vao.bind();
        vao.draw(GL11.GL_QUADS);

        this.lastRender = vao;
    }

    @Override
    public void end(MatrixStack matrixStack) {
        super.end(matrixStack);

        if (this.lastRender != null) {
            this.lastRender.unbind();
            this.lastRender = null;
        }
    }
}
