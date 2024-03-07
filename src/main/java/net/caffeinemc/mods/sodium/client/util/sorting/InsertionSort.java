package net.caffeinemc.mods.sodium.client.util.sorting;

public class InsertionSort extends AbstractSort {
    public static void insertionSort(final int[] indices, final int fromIndex, final int toIndex, final float[] keys) {
        int index = fromIndex;

        while (++index < toIndex) {
            int t = indices[index];
            int j = index;

            int u = indices[j - 1];

            while (keys[u] < keys[t]) {
                indices[j] = u;

                if (fromIndex == j - 1) {
                    --j;
                    break;
                }

                u = indices[--j - 1];
            }

            indices[j] = t;
        }
    }
}
