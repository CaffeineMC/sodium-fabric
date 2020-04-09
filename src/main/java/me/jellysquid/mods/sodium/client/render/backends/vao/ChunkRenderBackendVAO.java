package me.jellysquid.mods.sodium.client.render.backends.vao;

import me.jellysquid.mods.sodium.client.gl.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.GlVertexArrayBuffer;
import me.jellysquid.mods.sodium.client.render.backends.AbstractChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRender;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;

public class ChunkRenderBackendVAO extends AbstractChunkRenderBackend<ChunkRenderStateVAO> {
    private GlVertexArrayBuffer lastRender;

    @Override
    public ChunkRenderStateVAO createRenderState() {
        return new ChunkRenderStateVAO();
    }

    @Override
    public void render(ChunkRender<ChunkRenderStateVAO> chunk, RenderLayer layer, MatrixStack matrixStack, double x, double y, double z) {
        ChunkRenderStateVAO data = chunk.getRenderState();
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

    public static boolean isSupported() {
        return GlVertexArray.isSupported();
    }
}
