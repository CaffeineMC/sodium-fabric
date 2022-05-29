package net.caffeinemc.sodium.render.entity.shader;

import net.caffeinemc.gfx.api.shader.BufferBlock;
import net.caffeinemc.gfx.api.shader.BufferBlockType;
import net.caffeinemc.gfx.api.shader.ShaderBindingContext;

public class EntityShaderInterface {
    public final BufferBlock storageModelParts;
    public final BufferBlock storageModels;

    public EntityShaderInterface(ShaderBindingContext context) {
        this.storageModelParts = context.bindBufferBlock(BufferBlockType.STORAGE, 1);
        this.storageModels = context.bindBufferBlock(BufferBlockType.STORAGE, 2);
    }
}