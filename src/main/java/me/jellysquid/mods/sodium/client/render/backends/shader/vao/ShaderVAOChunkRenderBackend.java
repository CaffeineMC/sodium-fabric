package me.jellysquid.mods.sodium.client.render.backends.shader.vao;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.render.backends.shader.AbstractShaderChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;

/**
 * Shader-based render backend for chunks which uses VAOs to avoid the overhead in setting up vertex attribute pointers
 * before every draw call. This approach has significantly less CPU overhead as we only need to cross the native code
 * barrier once in order to setup all the necessary vertex attribute states and buffer bindings. Additionally, it might
 * allow the driver to skip validation logic that would otherwise be performed.
 */
public class ShaderVAOChunkRenderBackend extends AbstractShaderChunkRenderBackend<ShaderVAORenderState> {
    public ShaderVAOChunkRenderBackend(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        super(format);
    }

    @Override
    public void render(Iterator<ShaderVAORenderState> renders, MatrixStack matrixStack, double x, double y, double z) {
        this.begin(matrixStack);

        int chunkX = (int) (x / 16.0D);
        int chunkY = (int) (y / 16.0D);
        int chunkZ = (int) (z / 16.0D);

        this.activeProgram.setupModelViewMatrix(matrixStack, x % 16.0D, y % 16.0D, z % 16.0D);

        ShaderVAORenderState lastRender = null;

        while (renders.hasNext()) {
            ShaderVAORenderState vao = renders.next();

            if (vao != null) {
                this.activeProgram.setupChunk(vao.getOrigin(), chunkX, chunkY, chunkZ);

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
    protected ShaderVAORenderState createRenderState(GlBuffer buffer, ChunkRenderContainer<ShaderVAORenderState> render) {
        return new ShaderVAORenderState(buffer, render.getChunkPos());
    }
}
