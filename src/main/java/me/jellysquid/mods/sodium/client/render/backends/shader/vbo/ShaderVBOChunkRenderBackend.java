package me.jellysquid.mods.sodium.client.render.backends.shader.vbo;

import me.jellysquid.mods.sodium.client.gl.attribute.GlAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.render.backends.shader.AbstractShaderChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRender;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.Iterator;

public class ShaderVBOChunkRenderBackend extends AbstractShaderChunkRenderBackend<ShaderVBORenderState> {
    @Override
    public void render(Iterator<ShaderVBORenderState> renders, MatrixStack matrixStack, double x, double y, double z) {
        super.begin(matrixStack);

        for (GlAttributeBinding binding : this.activeProgram.attributes) {
            GL20.glEnableVertexAttribArray(binding.index);
        }

        this.activeProgram.setModelMatrix(matrixStack.peek());

        ShaderVBORenderState lastRender = null;

        while (renders.hasNext()) {
            ShaderVBORenderState vbo = renders.next();

            if (vbo == null) {
                return;
            }

            this.activeProgram.setModelOffset(vbo.getTranslation(), x, y, z);

            vbo.bind(this.activeProgram.attributes);
            vbo.draw(GL11.GL_QUADS);

            lastRender = vbo;
        }

        if (lastRender != null) {
            lastRender.unbind();
        }

        for (GlAttributeBinding binding : this.activeProgram.attributes) {
            GL20.glDisableVertexAttribArray(binding.index);
        }

        super.end(matrixStack);
    }

    @Override
    public Class<ShaderVBORenderState> getRenderStateType() {
        return ShaderVBORenderState.class;
    }

    @Override
    protected ShaderVBORenderState createRenderState(GlBuffer buffer, ChunkRender<ShaderVBORenderState> render) {
        return new ShaderVBORenderState(buffer, render.getTranslation());
    }
}
