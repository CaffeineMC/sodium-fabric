package me.jellysquid.mods.sodium.client.render.backends.shader.vao;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.render.backends.shader.AbstractShaderChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRender;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;

public class ShaderVAOChunkRenderBackend extends AbstractShaderChunkRenderBackend<ShaderVAORenderState> {
    public ShaderVAOChunkRenderBackend(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        super(format);
    }

    @Override
    public void render(Iterator<ShaderVAORenderState> renders, MatrixStack matrixStack, double x, double y, double z) {
        this.begin(matrixStack);

        this.activeProgram.setModelMatrix(matrixStack.peek());

        ShaderVAORenderState lastRender = null;

        while (renders.hasNext()) {
            ShaderVAORenderState vao = renders.next();

            if (vao != null) {
                this.activeProgram.setModelOffset(vao.getTranslation(), x, y, z);

                vao.bind(this.activeProgram.attributes);
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
        return new ShaderVAORenderState(buffer, render.getTranslation());
    }
}
