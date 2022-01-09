package me.jellysquid.mods.sodium.render.immediate;

import me.jellysquid.mods.sodium.interop.vanilla.mixin.ShaderTexture;
import me.jellysquid.mods.sodium.opengl.shader.Program;
import me.jellysquid.mods.sodium.opengl.types.RenderState;

public class ImmediatePenderPhase {
    public final Program<VanillaShaderInterface> program;
    public final RenderState renderState;
    public final ShaderTexture[] textures;

    public ImmediatePenderPhase(Program<VanillaShaderInterface> program, RenderState renderState, ShaderTexture[] textures) {
        this.program = program;
        this.renderState = renderState;
        this.textures = textures;
    }
}
