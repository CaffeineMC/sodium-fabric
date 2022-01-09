package me.jellysquid.mods.sodium.interop.vanilla.mixin;

import me.jellysquid.mods.sodium.opengl.shader.Program;
import me.jellysquid.mods.sodium.render.immediate.VanillaShaderInterface;

public interface ShaderExtended {
    Program<VanillaShaderInterface> sodium$getShader();
}
