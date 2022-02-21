package me.jellysquid.mods.sodium.interop.vanilla.mixin;

import me.jellysquid.mods.sodium.opengl.shader.Program;
import me.jellysquid.mods.sodium.opengl.types.RenderState;
import me.jellysquid.mods.sodium.render.immediate.VanillaShaderInterface;
import org.joml.Vector4f;

public interface MultiPhaseParametersExtended {
    RenderState createRenderState();

    Program<VanillaShaderInterface> createProgram();

    ShaderTexture[] createShaderTextures();

    Vector4f getDefaultColorModulator();
}
