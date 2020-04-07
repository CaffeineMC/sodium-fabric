package me.jellysquid.mods.sodium.client.render.backends.vbo;

import me.jellysquid.mods.sodium.client.gl.GlVertexBuffer;
import me.jellysquid.mods.sodium.client.render.backends.AbstractChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRender;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;

public class ChunkRenderBackendVBO extends AbstractChunkRenderBackend<ChunkRenderStateVBO> {
    private GlVertexBuffer lastRender;

    @Override
    public ChunkRenderStateVBO createRenderState() {
        return new ChunkRenderStateVBO();
    }

    @Override
    public void render(ChunkRender<ChunkRenderStateVBO> chunk, RenderLayer layer, MatrixStack matrixStack, double x, double y, double z) {
        ChunkRenderStateVBO data = chunk.getRenderState();
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
