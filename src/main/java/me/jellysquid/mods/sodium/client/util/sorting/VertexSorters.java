package me.jellysquid.mods.sodium.client.util.sorting;

import com.mojang.blaze3d.systems.VertexSorter;
import org.joml.Vector3f;

public class VertexSorters {
    public static VertexSorter sortByDistance(Vector3f origin) {
        return new SortByDistance(origin);
    }

    private static class SortByDistance extends AbstractVertexSorter {
        private final Vector3f origin;

        private SortByDistance(Vector3f origin) {
            this.origin = origin;
        }

        @Override
        protected float getKey(Vector3f position) {
            return this.origin.distanceSquared(position);
        }
    }

    private static abstract class AbstractVertexSorter implements VertexSorter {
        private static final int RADIX_SORT_THRESHOLD = 64;

        @Override
        public final int[] sort(Vector3f[] positions) {
            if (positions.length >= RADIX_SORT_THRESHOLD) {
                return this.radixSort(positions);
            } else {
                return this.mergeSort(positions);
            }
        }

        private int[] radixSort(Vector3f[] positions) {
            final var keys = new int[positions.length];

            for (int index = 0; index < positions.length; index++) {
                keys[index] = RadixSort.Floats.createRadixKey(this.getKey(positions[index]));
            }

            return RadixSort.sort(keys);
        }

        private int[] mergeSort(Vector3f[] positions) {
            final var keys = new float[positions.length];

            for (int index = 0; index < positions.length; index++) {
                keys[index] = this.getKey(positions[index]);
            }

            return MergeSort.mergeSort(keys);
        }

        protected abstract float getKey(Vector3f object);
    }
}
