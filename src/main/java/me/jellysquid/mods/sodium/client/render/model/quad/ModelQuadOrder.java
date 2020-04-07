package me.jellysquid.mods.sodium.client.render.model.quad;

public enum ModelQuadOrder {
    NORMAL(new int[] { 0, 1, 2, 3 }),
    FLIP(new int[] { 1, 2, 3, 0 });

    private final int[] indices;

    ModelQuadOrder(int[] indices) {
        this.indices = indices;
    }

    public int getVertexIndex(int i) {
        return this.indices[i];
    }

    public static ModelQuadOrder orderOf(float[] brightnesses) {
        if (brightnesses[0] + brightnesses[2] > brightnesses[1] + brightnesses[3]) {
            return FLIP;
        } else {
            return NORMAL;
        }
    }
}
