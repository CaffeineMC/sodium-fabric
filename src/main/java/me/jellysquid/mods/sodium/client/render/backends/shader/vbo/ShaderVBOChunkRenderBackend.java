package me.jellysquid.mods.sodium.client.render.backends.shader.vbo;

import me.jellysquid.mods.sodium.client.gl.attribute.GlAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlVertexBuffer;
import me.jellysquid.mods.sodium.client.render.backends.shader.AbstractShaderChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRender;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class ShaderVBOChunkRenderBackend extends AbstractShaderChunkRenderBackend<ShaderVBORenderState> {
    private GlVertexBuffer lastRender;

    @Override
    public ShaderVBORenderState createRenderState() {
        return new ShaderVBORenderState(this.program.attributes, this.useImmutableStorage);
    }

    @Override
    public void begin(MatrixStack matrixStack) {
        super.begin(matrixStack);

        for (GlAttributeBinding binding : this.program.attributes) {
            GL20.glEnableVertexAttribArray(binding.index);
        }
    }

    @Override
    public void render(ChunkRender<ShaderVBORenderState> chunk, BlockRenderPass pass, MatrixStack matrixStack, double x, double y, double z) {
        GlVertexBuffer vbo = chunk.getRenderState().getDataForPass(pass);

        if (vbo == null) {
            return;
        }

        this.program.uploadModelMatrix(this.createModelMatrix(chunk, x, y, z));

        vbo.bind();
        vbo.draw(GL11.GL_QUADS);

        this.lastRender = vbo;
    }

    @Override
    public void end(MatrixStack matrixStack) {
        if (this.lastRender != null) {
            this.lastRender.unbind();
            this.lastRender = null;
        }

        for (GlAttributeBinding binding : this.program.attributes) {
            GL20.glDisableVertexAttribArray(binding.index);
        }

        super.end(matrixStack);
    }
}
