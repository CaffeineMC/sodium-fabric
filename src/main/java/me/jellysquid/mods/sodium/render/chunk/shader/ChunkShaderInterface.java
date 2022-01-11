package me.jellysquid.mods.sodium.render.chunk.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.opengl.shader.ShaderBindingContext;
import me.jellysquid.mods.sodium.opengl.shader.uniform.*;

/**
 * A forward-rendering shader program for chunks.
 */
public class ChunkShaderInterface {
    public final UniformMatrix4 uniformModelViewMatrix;
    public final UniformMatrix4 uniformProjectionMatrix;
    public final UniformMatrix4 uniformModelViewProjectionMatrix;

    public final UniformBlock bufferInstanceData;

    private final UniformFloatArray uFogColor;
    private final UniformFloat uFogStart;
    private final UniformFloat uFogEnd;

    public ChunkShaderInterface(ShaderBindingContext context) {
        this.uniformModelViewMatrix = context.bindUniform("u_ModelViewMatrix", UniformMatrix4.of());
        this.uniformProjectionMatrix = context.bindUniform("u_ProjectionMatrix", UniformMatrix4.of());
        this.uniformModelViewProjectionMatrix = context.bindUniform("u_ModelViewProjectionMatrix", UniformMatrix4.of());

        UniformInt uniformBlockTex = context.bindUniform("u_BlockTex", UniformInt.of());
        uniformBlockTex.setInt(0);

        UniformInt uniformLightTex = context.bindUniform("u_LightTex", UniformInt.of());
        uniformLightTex.setInt(1);

        this.bufferInstanceData = context.bindUniformBlock("ubo_InstanceData", 0);

        this.uFogColor = context.bindUniform("u_FogColor", UniformFloatArray.ofSize(4));
        this.uFogStart = context.bindUniform("u_FogStart", UniformFloat.of());
        this.uFogEnd = context.bindUniform("u_FogEnd", UniformFloat.of());
    }

    public void setFogUniforms() {
        this.uFogColor.setFloats(RenderSystem.getShaderFogColor());
        this.uFogStart.setFloat(RenderSystem.getShaderFogStart());
        this.uFogEnd.setFloat(RenderSystem.getShaderFogEnd());
    }
}
