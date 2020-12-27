package me.jellysquid.mods.sodium.client.model.vertex.formats.particle;

import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

public interface ParticleVertexSink extends VertexSink {
    VertexFormat VERTEX_FORMAT = VertexFormats.POSITION_TEXTURE_COLOR_LIGHT;

    void writeParticle(float x, float y, float z, float u, float v, int color, int light);
}
