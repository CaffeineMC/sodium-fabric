package net.caffeinemc.mods.sodium.client.util.sorting;


public class AbstractSort {
    protected static int[] createIndexBuffer(int length) {
        var indices = new int[length];

        for (int i = 0; i < length; i++) {
            indices[i] = i;
        }

        return indices;
    }
}
