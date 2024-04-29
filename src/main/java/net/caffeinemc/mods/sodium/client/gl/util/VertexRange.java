package net.caffeinemc.mods.sodium.client.gl.util;

/**
 * A vertex range to be used in an array of ranges where order represents the ordering in the buffer, but not necessarily the order of ModelQuadFacing. Each vertex range stores the facing index it represents to later allow for correct block face culling.
 */
public record VertexRange(int vertexCount, int facing) {
}
