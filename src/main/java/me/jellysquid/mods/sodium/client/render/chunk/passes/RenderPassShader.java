package me.jellysquid.mods.sodium.client.render.chunk.passes;

import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import net.minecraft.util.Identifier;

public class RenderPassShader {
    private final Identifier shaderName;
    private final ShaderConstants constants;

    public RenderPassShader(Identifier shaderName, ShaderConstants constants) {
        this.shaderName = shaderName;
        this.constants = constants;
    }

    public ShaderConstants getConstants() {
        return this.constants;
    }

    public Identifier getShaderName() {
        return this.shaderName;
    }
}
