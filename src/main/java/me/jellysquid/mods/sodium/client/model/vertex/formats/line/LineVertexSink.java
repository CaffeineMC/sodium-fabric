package me.jellysquid.mods.sodium.client.model.vertex.formats.line;

import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

public interface LineVertexSink extends VertexSink {
    VertexFormat VERTEX_FORMAT = VertexFormats.POSITION_COLOR;

    void vertexLine(float x, float y, float z, int color);

    default void vertexLine(float x, float y, float z, float r, float g, float b, float a) {
        this.vertexLine(x, y, z, ColorABGR.pack(r, g, b, a));
    }
}
