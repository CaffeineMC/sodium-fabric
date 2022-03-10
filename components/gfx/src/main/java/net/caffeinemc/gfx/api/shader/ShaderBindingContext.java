package net.caffeinemc.gfx.api.shader;

public interface ShaderBindingContext {
    UniformBlock bindUniformBlock(String name, int bindingPoint);
}
