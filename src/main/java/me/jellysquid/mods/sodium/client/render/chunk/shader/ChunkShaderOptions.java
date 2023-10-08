package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;

public record ChunkShaderOptions(ChunkFogMode fog, TerrainRenderPass pass, ChunkVertexType type) {
    /**
     * @deprecated Only kept for Iris/Oculus compatibility, do not use
     */
    @Deprecated
    @SuppressWarnings("unused")
    public ChunkShaderOptions(ChunkFogMode fog, TerrainRenderPass pass) {
        this(fog, pass, ChunkMeshFormats.COMPACT);
    }

    public ShaderConstants constants() {
        ShaderConstants.Builder constants = ShaderConstants.builder();
        constants.addAll(this.fog.getDefines());
        constants.add(this.type.getDefine());

        if (this.pass.supportsFragmentDiscard()) {
            constants.add("USE_FRAGMENT_DISCARD");
        }

        return constants.build();
    }
}
