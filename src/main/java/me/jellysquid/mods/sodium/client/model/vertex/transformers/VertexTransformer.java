package me.jellysquid.mods.sodium.client.model.vertex.transformers;

public interface VertexTransformer {
    default float transformTextureU(float u) {
        return u;
    }

    default float transformTextureV(float v) {
        return v;
    }
}
