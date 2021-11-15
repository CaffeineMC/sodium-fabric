package me.jellysquid.mods.sodium.interop.vanilla.model;

import net.minecraft.client.gl.VertexBuffer;

public interface VboBackedModel {
    VertexBuffer getBakedVertices();

    int getVertexCount();

    float[] getPrimitivePositions();

    int[] getPrimitivePartIds();
}
