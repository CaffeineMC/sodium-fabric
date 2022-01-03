package me.jellysquid.mods.sodium.render.chunk.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.opengl.buffer.Buffer;
import me.jellysquid.mods.sodium.opengl.shader.ShaderBindingContext;
import me.jellysquid.mods.sodium.opengl.shader.uniform.UniformBlock;
import me.jellysquid.mods.sodium.opengl.shader.uniform.UniformFloat3v;
import me.jellysquid.mods.sodium.opengl.shader.uniform.UniformInt;
import me.jellysquid.mods.sodium.opengl.shader.uniform.UniformMatrix4f;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexType;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL32C;

/**
 * A forward-rendering shader program for chunks.
 */
public class ChunkShaderInterface {
    private final UniformInt uniformBlockTex;
    private final UniformInt uniformLightTex;

    private final UniformMatrix4f uniformModelViewMatrix;
    private final UniformMatrix4f uniformProjectionMatrix;
    private final UniformFloat3v uniformRegionOffset;

    private final UniformBlock uniformBlockDrawParameters;

    // The fog shader component used by this program in order to setup the appropriate GL state
    private final ChunkShaderFogComponent fogShader;

    public ChunkShaderInterface(ShaderBindingContext context, ChunkShaderOptions options) {
        this.uniformModelViewMatrix = context.bindUniform("u_ModelViewMatrix", UniformMatrix4f::new);
        this.uniformProjectionMatrix = context.bindUniform("u_ProjectionMatrix", UniformMatrix4f::new);
        this.uniformRegionOffset = context.bindUniform("u_RegionOffset", UniformFloat3v::new);

        this.uniformBlockTex = context.bindUniform("u_BlockTex", UniformInt::new);
        this.uniformLightTex = context.bindUniform("u_LightTex", UniformInt::new);

        this.uniformBlockDrawParameters = context.bindUniformBlock("ubo_DrawParameters", 0);

        this.fogShader = options.fog().getFactory().apply(context);
    }

    public void setup() {
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

    public void setDrawUniforms(Buffer buffer) {
        this.uniformBlockDrawParameters.bindBuffer(buffer);
    }

    public void setRegionOffset(float x, float y, float z) {
        this.uniformRegionOffset.set(x, y, z);
    }
}
