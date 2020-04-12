package me.jellysquid.mods.sodium.client.render.backends.shader.vao;

import me.jellysquid.mods.sodium.client.gl.tessellation.GlVertexArrayWithBuffer;
import me.jellysquid.mods.sodium.client.render.backends.shader.AbstractShaderChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRender;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;

public class ShaderVAOChunkRenderBackend extends AbstractShaderChunkRenderBackend<ShaderVAORenderState> {
    private GlVertexArrayWithBuffer lastRender;

    @Override
    public void render(ChunkRender<ShaderVAORenderState> chunk, BlockRenderPass pass, MatrixStack matrixStack, double x, double y, double z) {
        GlVertexArrayWithBuffer vao = chunk.getRenderState().getDataForPass(pass);

        if (vao == null) {
            return;
        }

        this.program.uploadModelMatrix(this.createModelMatrix(chunk, x, y, z));

        vao.bind();
        vao.draw(GL11.GL_QUADS);

        this.lastRender = vao;
    }

    @Override
    public void end(MatrixStack matrixStack) {
        if (this.lastRender != null) {
            this.lastRender.unbind();
            this.lastRender = null;
        }

        super.end(matrixStack);
    }

    @Override
    public ShaderVAORenderState createRenderState() {
        return new ShaderVAORenderState(this.program.attributes, this.useImmutableStorage);
    }
}
