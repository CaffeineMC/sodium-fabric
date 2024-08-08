package net.caffeinemc.mods.sodium.client.model.quad.properties;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;

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
    public static ModelQuadOrientation orientByBrightness(float[] brightnesses, int[] lightmaps) {
        // If one side of the quad is brighter, flip the sides
        float br02 = brightnesses[0] + brightnesses[2];
        float br13 = brightnesses[1] + brightnesses[3];
        if (br02 > br13) {
            return NORMAL;
        } else if (br02 < br13) {
            return FLIP;
        }

        // If one side of the quad is darker, flip the sides
        int lm02 = lightmaps[0] + lightmaps[2];
        int lm13 = lightmaps[1] + lightmaps[3];
        if (lm02 <= lm13) {
            return NORMAL;
        } else {
            return FLIP;
        }
    }

    /**
     * Determines the orientation of the vertices in the quad.
     */
    public static ModelQuadOrientation orientByBrightness(float[] brightnesses, QuadView quad) {
        // If one side of the quad is brighter, flip the sides
        float br02 = brightnesses[0] + brightnesses[2];
        float br13 = brightnesses[1] + brightnesses[3];
        if (br02 > br13) {
            return NORMAL;
        } else if (br02 < br13) {
            return FLIP;
        }

        // If one side of the quad is darker, flip the sides
        int lm02 = quad.lightmap(0) + quad.lightmap(2);
        int lm13 = quad.lightmap(1) + quad.lightmap(3);
        if (lm02 <= lm13) {
            return NORMAL;
        } else {
            return FLIP;
        }
    }
}
