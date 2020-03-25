package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.gl.GlVertexBuffer;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;

public class ChunkRendererVBO implements ChunkRenderer<ChunkRenderDataVBO> {
    @Override
    public ChunkRenderDataVBO createRenderData() {
        return new ChunkRenderDataVBO();
    }

    @Override
    public void begin() {

    }

    @Override
    public void render(MatrixStack matrixStack, RenderLayer layer, ChunkRenderDataVBO chunk) {
        GlVertexBuffer vertexBuffer = chunk.getVertexBufferForLayer(layer);
        vertexBuffer.bind();

        VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL.startDrawing(0L);

        vertexBuffer.draw(matrixStack.peek().getModel(), GL11.GL_QUADS);
    }

    @Override
    public void end() {
        VertexBuffer.unbind();
    }
}
