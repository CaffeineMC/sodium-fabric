package me.jellysquid.mods.sodium.client.render.backends.shader.vao;

import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.render.backends.shader.AbstractShaderChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRender;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;

public class ShaderVAOChunkRenderBackend extends AbstractShaderChunkRenderBackend<ShaderVAORenderState> {
    @Override
    public void render(Iterator<ShaderVAORenderState> renders, MatrixStack matrixStack, double x, double y, double z) {
        this.begin(matrixStack);

        this.program.setMatrices(matrixStack.peek());

        ShaderVAORenderState lastRender = null;

        while (renders.hasNext()) {
            ShaderVAORenderState vao = renders.next();

            if (vao != null) {
                this.program.setModelOffset(vao.getTranslation(), x, y, z);

                vao.bind();
                vao.draw(GL11.GL_QUADS);

                lastRender = vao;
            }
        }

        if (lastRender != null) {
            lastRender.unbind();
        }

        this.end(matrixStack);
    }

    @Override
    public Class<ShaderVAORenderState> getRenderStateType() {
        return ShaderVAORenderState.class;
    }

    @Override
    protected ShaderVAORenderState createRenderState(GlBuffer buffer, ChunkRender<ShaderVAORenderState> render) {
        return new ShaderVAORenderState(buffer, this.program.attributes, render.getTranslation());
    }
}
