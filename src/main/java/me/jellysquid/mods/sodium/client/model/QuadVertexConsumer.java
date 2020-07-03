package me.jellysquid.mods.sodium.client.model;

public interface QuadVertexConsumer {
    /**
     * Writes a vertex directly into the consumer with no additional processing. This requires callers to do some
     * upfront work to encode their values.
     *
     * @param x       The x-position of the vertex
     * @param y       The y-position of the vertex
     * @param z       The z-position of the vertex
     * @param color   The color of the vertex in little-endian RGBA format
     * @param u       The u-position of the texture
     * @param v       The v-position of the texture
     * @param overlay The overlay (shadow) of the vertex
     * @param light   The light of the vertex
     * @param normal  The normal of the vertex
     */
    void vertexQuad(float x, float y, float z, int color, float u, float v, int overlay, int light, int normal);
}
