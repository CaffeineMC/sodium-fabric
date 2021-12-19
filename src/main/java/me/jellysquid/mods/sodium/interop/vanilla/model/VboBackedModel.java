package me.jellysquid.mods.sodium.interop.vanilla.model;

import me.jellysquid.mods.thingl.tessellation.Tessellation;

public interface VboBackedModel {
    Tessellation getBakedVertices();

    int getVertexCount();

    float[] getPrimitivePositions();

    int[] getPrimitivePartIds();
}
