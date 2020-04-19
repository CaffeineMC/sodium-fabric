package me.jellysquid.mods.sodium.client.render.model.quad;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;

import java.nio.ByteBuffer;

public interface ModelQuadEncoder {
    void write(GlVertexFormat<?> format, ByteBuffer buffer, int position, ModelQuadViewMutable quad);
}
