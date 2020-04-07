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
    public void render(ChunkRender<ChunkRenderDataVBO> chunk, RenderLayer layer, MatrixStack matrixStack, double x, double y, double z) {
        ChunkRenderDataVBO data = chunk.getRenderData();
        GlVertexBuffer vbo = data.getVertexBufferForLayer(layer);

        if (vbo == null) {
            return;
        }

        this.beginChunkRender(chunk, x, y, z);

        vbo.bind();
        VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL.startDrawing(0L);
        vbo.draw(matrixStack.peek().getModel(), GL11.GL_QUADS);

        this.lastRender = vbo;
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
