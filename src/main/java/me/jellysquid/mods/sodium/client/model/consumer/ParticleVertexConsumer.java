package me.jellysquid.mods.sodium.client.model.consumer;

public interface ParticleVertexConsumer {
    /**
     * Writes a vertex directly into the consumer with no additional processing. This requires callers to do some
     * upfront work to encode their values.
     *
     * @param x     The x-position of the vertex
     * @param y     The y-position of the vertex
     * @param z     The z-position of the vertex
     * @param u     The u-position of the texture
     * @param v     The v-position of the texture
     * @param color The color of the vertex in little-endian RGBA format
     * @param light The light of the vertex
     */
    void vertexParticle(float x, float y, float z, float u, float v, int color, int light);

}
