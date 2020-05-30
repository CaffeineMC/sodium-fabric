package me.jellysquid.mods.sodium.client.render.backends.shader.vbo;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.render.backends.shader.AbstractShaderChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.Iterator;

/**
 * A simple chunk rendering backend which mirrors that of vanilla's own pretty closely.
 */
public class ShaderVBOChunkRenderBackend extends AbstractShaderChunkRenderBackend<ShaderVBORenderState> {
    public ShaderVBOChunkRenderBackend(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        super(format);
    }

    @Override
    public void render(Iterator<ShaderVBORenderState> renders, MatrixStack matrixStack, double x, double y, double z) {
        super.begin(matrixStack);

        for (GlVertexAttributeBinding binding : this.activeProgram.attributes) {
            GL20.glEnableVertexAttribArray(binding.index);
        }

        int chunkX = (int) (x / 16.0D);
        int chunkY = (int) (y / 16.0D);
        int chunkZ = (int) (z / 16.0D);

        this.activeProgram.setupModelViewMatrix(matrixStack, x % 16.0D, y % 16.0D, z % 16.0D);

        ShaderVBORenderState lastRender = null;

        while (renders.hasNext()) {
            ShaderVBORenderState vbo = renders.next();

            if (vbo == null) {
                return;
            }

            this.activeProgram.setupChunk(vbo.getOrigin(), chunkX, chunkY, chunkZ);

            vbo.bind(this.activeProgram.attributes);
            vbo.draw(GL11.GL_QUADS);

            lastRender = vbo;
        }

        if (lastRender != null) {
            lastRender.unbind();
        }

        for (GlVertexAttributeBinding binding : this.activeProgram.attributes) {
            GL20.glDisableVertexAttribArray(binding.index);
        }

        super.end(matrixStack);
    }

    @Override
    public Class<ShaderVBORenderState> getRenderStateType() {
        return ShaderVBORenderState.class;
    }

    @Override
    protected ShaderVBORenderState createRenderState(GlBuffer buffer, ChunkRenderContainer<ShaderVBORenderState> render) {
        return new ShaderVBORenderState(buffer, render.getChunkPos());
    }
}
