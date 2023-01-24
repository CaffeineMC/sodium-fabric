package me.jellysquid.mods.sodium.client.render.chunk.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.*;
import me.jellysquid.mods.sodium.client.render.vertex.type.ChunkVertexType;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL32C;

/**
 * A forward-rendering shader program for chunks.
 */
public class ChunkShaderInterface {
    private final GlUniformInt uniformBlockTex;
    private final GlUniformInt uniformLightTex;

    private final GlUniformMatrix4f uniformModelViewMatrix;
    private final GlUniformMatrix4f uniformProjectionMatrix;
    private final GlUniformFloat3v uniformRegionOffset;

    private final GlUniformBlock uniformBlockDrawParameters;

    // The fog shader component used by this program in order to setup the appropriate GL state
    private final ChunkShaderFogComponent fogShader;

    public ChunkShaderInterface(ShaderBindingContext context, ChunkShaderOptions options) {
        this.uniformModelViewMatrix = context.bindUniform("u_ModelViewMatrix", GlUniformMatrix4f::new);
        this.uniformProjectionMatrix = context.bindUniform("u_ProjectionMatrix", GlUniformMatrix4f::new);
        this.uniformRegionOffset = context.bindUniform("u_RegionOffset", GlUniformFloat3v::new);

        this.uniformBlockTex = context.bindUniform("u_BlockTex", GlUniformInt::new);
        this.uniformLightTex = context.bindUniform("u_LightTex", GlUniformInt::new);

        this.uniformBlockDrawParameters = context.bindUniformBlock("ubo_DrawParameters", 0);

        this.fogShader = options.fog().getFactory().apply(context);
    }

    public void setup(ChunkVertexType vertexType) {
        RenderSystem.activeTexture(GL32C.GL_TEXTURE0);
        RenderSystem.bindTexture(RenderSystem.getShaderTexture(0));

        RenderSystem.activeTexture(GL32C.GL_TEXTURE2);
        RenderSystem.bindTexture(RenderSystem.getShaderTexture(2));

        this.uniformBlockTex.setInt(0);
        this.uniformLightTex.setInt(2);

        this.fogShader.setup();
    }

    public void setProjectionMatrix(Matrix4f matrix) {
        this.uniformProjectionMatrix.set(matrix);
    }

    public void setModelViewMatrix(Matrix4f matrix) {
        this.uniformModelViewMatrix.set(matrix);
    }

    public void setDrawUniforms(GlMutableBuffer buffer) {
        this.uniformBlockDrawParameters.bindBuffer(buffer);
    }

    public void setRegionOffset(float x, float y, float z) {
        this.uniformRegionOffset.set(x, y, z);
    }
}
