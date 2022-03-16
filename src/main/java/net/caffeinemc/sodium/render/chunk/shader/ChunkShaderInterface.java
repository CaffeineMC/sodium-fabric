package net.caffeinemc.sodium.render.chunk.shader;

import net.caffeinemc.gfx.api.shader.ShaderBindingContext;
import net.caffeinemc.gfx.api.shader.BufferBlock;

/**
 * A forward-rendering shader program for chunks.
 */
public class ChunkShaderInterface {
    public final BufferBlock uniformCameraMatrices;
    public final BufferBlock uniformFogParameters;
    public final BufferBlock uniformInstanceData;

    public ChunkShaderInterface(ShaderBindingContext context) {
        this.uniformCameraMatrices = context.bindUniformBlock("ubo_CameraMatrices", 0);
        this.uniformFogParameters = context.bindUniformBlock("ubo_FogParameters", 1);
        this.uniformInstanceData = context.bindUniformBlock("ubo_InstanceData", 2);
    }
}
