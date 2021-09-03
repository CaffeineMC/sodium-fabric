package me.jellysquid.mods.sodium.render.chunk.format;

import me.jellysquid.mods.sodium.model.vertex.VertexSink;
import me.jellysquid.mods.sodium.render.chunk.material.MaterialFlag;
import me.jellysquid.mods.sodium.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.util.color.ColorABGR;
import me.jellysquid.mods.sodium.util.color.ColorARGB;

public interface ModelVertexSink extends VertexSink {
    /**
     * Writes a model vertex to the vertex sink.
     *
     * @param position The encoded position coordinate
     *                 See {@link ModelVertexCompression#encodePositionAttribute(float, float, float)}
     * @param color The ABGR-encoded color value
     *              See {@link ColorABGR#pack(int, int, int, int)} and {@link ColorARGB#toABGR(int)}
     * @param blockTexture The encoded block texture coordinates
     *                     See {@link ModelVertexCompression#encodeTextureAttribute(float, float)}
     * @param lightTexture The encoded light texture coordinates
     *                     See {@link ModelVertexCompression#encodeLightMapTexCoord(int)}
     * @param chunkIndex The relative local index of the chunk this vertex belongs to
     *                   See {@link RenderRegion#getChunkIndex(int, int, int)}
     * @param materialBits The material bits for this vertex
     *                     See {@link MaterialFlag}
     */
    void writeVertex(long position, int color, int blockTexture, int lightTexture, short chunkIndex, int materialBits);
}
