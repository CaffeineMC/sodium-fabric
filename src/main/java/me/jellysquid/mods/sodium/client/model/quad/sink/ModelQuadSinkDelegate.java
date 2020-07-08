package me.jellysquid.mods.sodium.client.model.quad.sink;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;

public interface ModelQuadSinkDelegate {
    ModelQuadSink get(ModelQuadFacing facing);
}
