package me.jellysquid.mods.sodium.opengl.shader;

import me.jellysquid.mods.sodium.opengl.shader.uniform.Uniform;
import me.jellysquid.mods.sodium.opengl.shader.uniform.UniformBlock;

import java.util.function.IntFunction;

public interface ShaderBindingContext {
    <U extends Uniform<?>> U bindUniform(String name, IntFunction<U> factory);

    UniformBlock bindUniformBlock(String name, int bindingPoint);
}
