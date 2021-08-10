package me.jellysquid.mods.sodium.client.render.chunk.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformBlock;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformFloat;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformInt;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformMatrix4f;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import org.lwjgl.opengl.GL32C;

/**
 * A forward-rendering shader program for chunks.
 */
public class ChunkProgram extends GlProgram {
    private final GlUniformFloat uniformModelScale;
    private final GlUniformFloat uniformModelOffset;
    private final GlUniformFloat uniformTextureScale;

    private final GlUniformInt uniformBlockTex;
    private final GlUniformInt uniformLightTex;

    public final GlUniformMatrix4f uniformModelViewMatrix;
    public final GlUniformMatrix4f uniformProjectionMatrix;

    public final GlUniformBlock uniformBlockDrawParameters;

    // The fog shader component used by this program in order to setup the appropriate GL state
    private final ChunkShaderFogComponent fogShader;

    public ChunkProgram(RenderDevice owner, int handle, ChunkShaderOptions options) {
        super(handle);

        this.uniformModelViewMatrix = this.bindUniform("u_ModelViewMatrix", GlUniformMatrix4f::new);
        this.uniformProjectionMatrix = this.bindUniform("u_ProjectionMatrix", GlUniformMatrix4f::new);

        this.uniformBlockTex = this.bindUniform("u_BlockTex", GlUniformInt::new);
        this.uniformLightTex = this.bindUniform("u_LightTex", GlUniformInt::new);

        this.uniformModelScale = this.bindUniform("u_ModelScale", GlUniformFloat::new);
        this.uniformModelOffset = this.bindUniform("u_ModelOffset", GlUniformFloat::new);
        this.uniformTextureScale = this.bindUniform("u_TextureScale", GlUniformFloat::new);

        this.uniformBlockDrawParameters = this.bindUniformBlock("ubo_DrawParameters", 0);

        this.fogShader = options.fog().getFactory().apply(this);
    }

    public void setup(ChunkVertexType vertexType) {
        RenderSystem.activeTexture(GL32C.GL_TEXTURE0);
        RenderSystem.bindTexture(RenderSystem.getShaderTexture(0));

        RenderSystem.activeTexture(GL32C.GL_TEXTURE2);
        RenderSystem.bindTexture(RenderSystem.getShaderTexture(2));

        this.uniformBlockTex.setInt(0);
        this.uniformLightTex.setInt(2);

        this.uniformModelScale.setFloat(vertexType.getModelScale());
        this.uniformModelOffset.setFloat(vertexType.getModelOffset());
        this.uniformTextureScale.setFloat(vertexType.getTextureScale());
        
        this.fogShader.setup();
    }
}
