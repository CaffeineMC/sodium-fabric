package me.jellysquid.mods.sodium.client.render.model.quad;

import java.nio.ByteBuffer;

public interface ModelQuadEncoder {
    void write(ByteBuffer buffer, int position, ModelQuadViewMutable quad, float x, float y, float z);
}
