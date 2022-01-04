package me.jellysquid.mods.sodium.opengl.shader;

import me.jellysquid.mods.sodium.opengl.shader.uniform.Uniform;
import me.jellysquid.mods.sodium.opengl.shader.uniform.UniformBlock;
import me.jellysquid.mods.sodium.opengl.shader.uniform.UniformFactory;

public interface ShaderBindingContext {
    <U extends Uniform> U bindUniform(String name, UniformFactory<U> factory);

    UniformBlock bindUniformBlock(String name, int bindingPoint);
}
