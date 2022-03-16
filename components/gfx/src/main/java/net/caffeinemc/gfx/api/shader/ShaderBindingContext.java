package net.caffeinemc.gfx.api.shader;

public interface ShaderBindingContext {
    BufferBlock bindUniformBlock(String name, int bindingPoint);
}
