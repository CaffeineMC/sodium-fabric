package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkRender;
import me.jellysquid.mods.sodium.client.render.gl.GlVertexArrayBuffer;
import me.jellysquid.mods.sodium.client.render.matrix.MatrixUtil;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

public class ChunkRendererVAO extends AbstractChunkRenderer<ChunkRenderDataVAO> {
    private GlVertexArrayBuffer lastRender;

    @Override
    public ChunkRenderDataVAO createRenderData() {
        return new ChunkRenderDataVAO();
    }

    @Override
    public void begin() {

    }

    @Override
    public void render(ChunkRender<ChunkRenderDataVAO> chunk, RenderLayer layer, MatrixStack matrixStack, double x, double y, double z) {
        ChunkRenderDataVAO data = chunk.getRenderData();
        GlVertexArrayBuffer vao = data.getVertexArrayForLayer(layer);

        if (vao == null) {
            return;
        }

        this.beginChunkRender(matrixStack, chunk, x, y, z);

        FloatBuffer modelMatrix = MatrixUtil.writeToBuffer(matrixStack.peek().getModel());

        GL11.glLoadMatrixf(modelMatrix);

        vao.bind();
        vao.draw(GL11.GL_QUADS);

        this.endChunkRender(matrixStack, chunk, x, y, z);

        this.lastRender = vao;
    }

    @Override
    public void end() {
        if (this.lastRender != null) {
            this.lastRender.unbind();
            this.lastRender = null;
        }
    }
}
