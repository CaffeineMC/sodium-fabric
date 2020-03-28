package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkRender;
import me.jellysquid.mods.sodium.client.render.gl.GlVertexBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;

public class ChunkRendererVBO extends AbstractChunkRenderer<ChunkRenderDataVBO> {
    private GlVertexBuffer lastRender;

    @Override
    public ChunkRenderDataVBO createRenderData() {
        return new ChunkRenderDataVBO();
    }

    @Override
    public void begin() {

    }

    @Override
    public void render(ChunkRender<ChunkRenderDataVBO> chunk, RenderLayer layer, MatrixStack matrixStack, double x, double y, double z) {
        ChunkRenderDataVBO data = chunk.getRenderData();

        if (data == null) {
            return;
        }

        GlVertexBuffer vbo = data.getVertexBufferForLayer(layer);

        if (vbo == null) {
            return;
        }

        this.beginChunkRender(matrixStack, chunk, x, y, z);

        vbo.bind();
        VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL.startDrawing(0L);
        vbo.draw(matrixStack.peek().getModel(), GL11.GL_QUADS);

        this.endChunkRender(matrixStack, chunk, x, y, z);

        this.lastRender = vbo;
    }

    @Override
    public void end() {
        if (this.lastRender != null) {
            this.lastRender.unbind();
            this.lastRender = null;
        }
    }
}
