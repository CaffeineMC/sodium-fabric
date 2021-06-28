package me.jellysquid.mods.sodium.client.model.quad.properties;

/**
 * Defines the orientation of vertices in a model quad. This information be used to re-orient the quad's vertices to a
 * consistent order, eliminating a number of shading issues caused by anisotropy problems.
 */
public enum ModelQuadOrientation {
    NORMAL(new int[] { 0, 1, 2, 3 }),
    FLIP(new int[] { 1, 2, 3, 0 });

    private final int[] indices;

    ModelQuadOrientation(int[] indices) {
        this.indices = indices;
    }

    /**
     * @return The re-oriented index of the vertex {@param idx}
     */
    public int getVertexIndex(int idx) {
        return this.indices[idx];
    }

    /**
     * Determines the orientation of the vertices in the quad.
     */
    public static ModelQuadOrientation orientByBrightness(float[] brightnesses) {
        // If one side of the quad is brighter, flip the sides
        if (brightnesses[0] + brightnesses[2] > brightnesses[1] + brightnesses[3]) {
            return NORMAL;
        } else {
            return FLIP;
        }
    }
}
