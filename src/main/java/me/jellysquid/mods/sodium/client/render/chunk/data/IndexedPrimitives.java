package me.jellysquid.mods.sodium.client.render.chunk.data;

import org.joml.Vector3f;

public class IndexedPrimitives {
    public final Vector3f[] centers;
    public int[] indexes;

    public IndexedPrimitives(Vector3f[] centers) {
        this.centers = centers;
        this.indexes = new int[centers.length];
        for (int i = 0; i < centers.length; i++) {
            this.indexes[i] = i;
        }
    }
}
