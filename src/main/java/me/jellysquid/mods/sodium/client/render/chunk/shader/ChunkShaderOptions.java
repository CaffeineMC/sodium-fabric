package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.passes.RenderPassShader;
import net.minecraft.util.Identifier;

public record ChunkShaderOptions(ChunkFogMode fog, BlockRenderPass pass) {
    public Identifier getVertexShaderName() {
        return this.pass.getVertexShader().getShaderName();
    }

    public Identifier getFragmentShaderName() {
        return this.pass.getFragmentShader().getShaderName();
    }

    public ShaderConstants getVertexShaderConstants() {
        ShaderConstants.Builder constants = ShaderConstants.buildFrom(this.pass.getVertexShader().getConstants());
        constants.addAll(this.fog.getDefines());

        return constants.build();
    }

    public ShaderConstants getFragmentShaderConstants() {
        ShaderConstants.Builder constants = ShaderConstants.buildFrom(this.pass.getFragmentShader().getConstants());
        constants.addAll(this.fog.getDefines());

        return constants.build();
    }
}
