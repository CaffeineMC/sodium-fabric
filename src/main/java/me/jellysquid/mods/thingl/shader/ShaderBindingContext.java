package me.jellysquid.mods.thingl.shader;

import me.jellysquid.mods.thingl.shader.uniform.GlUniform;
import me.jellysquid.mods.thingl.shader.uniform.GlUniformBlock;

import java.util.function.IntFunction;

public interface ShaderBindingContext {
    <U extends GlUniform<?>> U bindUniform(String name, IntFunction<U> factory);

    GlUniformBlock bindUniformBlock(String name, int bindingPoint);
}
