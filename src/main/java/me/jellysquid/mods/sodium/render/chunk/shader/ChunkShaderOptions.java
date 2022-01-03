package me.jellysquid.mods.sodium.render.chunk.shader;

import me.jellysquid.mods.sodium.render.shader.ShaderConstants;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexType;
import me.jellysquid.mods.sodium.render.chunk.passes.ChunkRenderPass;

public record ChunkShaderOptions(ChunkFogMode fog, ChunkRenderPass pass, TerrainVertexType vertexType) {
    public ShaderConstants constants() {
        ShaderConstants.Builder constants = ShaderConstants.builder();
        constants.addAll(this.fog.getDefines());

        if (this.pass.getAlphaCutoff() != 0.0f) {
            constants.add("ALPHA_CUTOFF", String.valueOf(this.pass.getAlphaCutoff()));
        }

        constants.add("USE_VERTEX_COMPRESSION"); // TODO: allow compact vertex format to be disabled
        constants.add("VERT_POS_SCALE", String.valueOf(this.vertexType.getPositionScale()));
        constants.add("VERT_POS_OFFSET", String.valueOf(this.vertexType.getPositionOffset()));
        constants.add("VERT_TEX_SCALE", String.valueOf(this.vertexType.getTextureScale()));

        return constants.build();
    }
}
