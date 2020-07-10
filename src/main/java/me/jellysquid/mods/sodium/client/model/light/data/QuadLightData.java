package me.jellysquid.mods.sodium.client.model.light.data;

/**
 * Stores the computed light data for a block model quad. The vertex order of each array is defined as that of the
 * quad's vertex order.
 */
public class QuadLightData {
    /**
     * The brightness of each vertex in the quad as normalized floats.
     */
    public final float[] br = new float[4];

    /**
     * The lightmap texture coordinates for each vertex in the quad.
     */
    public final int[] lm = new int[4];
}
