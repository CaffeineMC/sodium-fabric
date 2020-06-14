package me.jellysquid.mods.sodium.client.model;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadSink;

public interface ModelQuadSinkDelegate {
    ModelQuadSink get(ModelQuadFacing facing);
}
