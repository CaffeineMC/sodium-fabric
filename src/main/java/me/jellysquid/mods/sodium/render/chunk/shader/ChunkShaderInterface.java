package me.jellysquid.mods.sodium.render.chunk.shader;

import net.caffeinemc.gfx.api.shader.ShaderBindingContext;
import net.caffeinemc.gfx.api.shader.UniformBlock;

/**
 * A forward-rendering shader program for chunks.
 */
public class ChunkShaderInterface {
    public final UniformBlock uniformCameraMatrices;
    public final UniformBlock uniformFogParameters;
    public final UniformBlock uniformInstanceData;

    public ChunkShaderInterface(ShaderBindingContext context) {
        this.uniformCameraMatrices = context.bindUniformBlock("ubo_CameraMatrices", 0);
        this.uniformFogParameters = context.bindUniformBlock("ubo_FogParameters", 1);
        this.uniformInstanceData = context.bindUniformBlock("ubo_InstanceData", 2);
    }
}
