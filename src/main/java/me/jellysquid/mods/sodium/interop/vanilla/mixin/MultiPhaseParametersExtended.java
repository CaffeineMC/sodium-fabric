package me.jellysquid.mods.sodium.interop.vanilla.mixin;

import me.jellysquid.mods.sodium.opengl.shader.Program;
import me.jellysquid.mods.sodium.opengl.types.RenderState;
import me.jellysquid.mods.sodium.render.immediate.VanillaShaderInterface;

public interface MultiPhaseParametersExtended {
    RenderState createRenderState();

    Program<VanillaShaderInterface> createProgram();

    ShaderTexture[] createShaderTextures();
}
