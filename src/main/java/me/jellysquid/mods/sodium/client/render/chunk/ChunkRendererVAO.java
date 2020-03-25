package me.jellysquid.mods.sodium.client.render.chunk;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;

public class ChunkRendererVAO implements ChunkRenderer<ChunkRenderDataVAO> {
    @Override
    public ChunkRenderDataVAO createRenderData() {
        return new ChunkRenderDataVAO();
    }

    @Override
    public void begin() {

    }

    @Override
    public void render(MatrixStack matrixStack, RenderLayer layer, ChunkRenderDataVAO chunk) {
        VertexBufferWithArray vao = chunk.getVertexArrayForLayer(layer);
        vao.bind();
        vao.draw(matrixStack.peek().getModel(), GL11.GL_QUADS);
    }

    @Override
    public void end() {
        VertexBufferWithArray.unbind();
    }
}
