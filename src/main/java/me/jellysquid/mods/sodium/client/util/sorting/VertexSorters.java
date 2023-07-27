package me.jellysquid.mods.sodium.client.util.sorting;

import com.mojang.blaze3d.systems.VertexSorter;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;

import org.joml.Vector3f;

public class VertexSorters {
    public static VertexSorter sortByDistance(Vector3f origin) {
        return new SortByDistance(origin);
    }

    public static VertexSorter sortByAxis(ModelQuadFacing facing) {
        switch (facing) {
            case DOWN:
                return new SortByAxis(1, -1.0F);
            case UP:
                return new SortByAxis(1, 1.0F);
            case NORTH:
                return new SortByAxis(2, -1.0F);
            case SOUTH:
                return new SortByAxis(2, 1.0F);
            case WEST:
                return new SortByAxis(0, -1.0F);
            case EAST:
                return new SortByAxis(0, 1.0F);
            default:
                throw new IllegalArgumentException("Invalid facing: " + facing);
        }
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

    private static class SortByAxis extends AbstractVertexSorter {
        private final int axis;
        private final float sign;

        private SortByAxis(int axis, float sign) {
            this.axis = axis;
            this.sign = sign;
        }

        @Override
        protected float getKey(Vector3f position) {
            return sign * position.get(this.axis);
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
