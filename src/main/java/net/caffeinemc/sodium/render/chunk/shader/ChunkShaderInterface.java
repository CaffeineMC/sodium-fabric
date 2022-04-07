package net.caffeinemc.sodium.render.chunk.shader;

import net.caffeinemc.gfx.api.shader.ShaderBindingContext;
import net.caffeinemc.gfx.api.shader.BufferBlock;

/**
 * A forward-rendering shader program for chunks.
 */
public class ChunkShaderInterface {
    public final BufferBlock uniformCameraMatrices;
    public final BufferBlock uniformInstanceData;
    public final BufferBlock uniformFogParametersVS;
    public final BufferBlock uniformFogParametersFS;

    public ChunkShaderInterface(ShaderBindingContext context) {
        this.uniformCameraMatrices = context.bindUniformBlock(0);
        this.uniformInstanceData = context.bindUniformBlock(1);
        this.uniformFogParametersVS = context.bindUniformBlock(2);
        this.uniformFogParametersFS = context.bindUniformBlock(3);
    }
}
