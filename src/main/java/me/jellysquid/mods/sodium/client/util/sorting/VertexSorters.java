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
            case POS_X:
                return new SortByAxis(0, 1.0F);
            case NEG_X:
                return new SortByAxis(0, -1.0F);
            case POS_Y:
                return new SortByAxis(1, 1.0F);
            case NEG_Y:
                return new SortByAxis(1, -1.0F);
            case POS_Z:
                return new SortByAxis(2, 1.0F);
            case NEG_Z:
                return new SortByAxis(2, -1.0F);
            default:
                throw new IllegalArgumentException("Unknown facing: " + facing);
        }
    }

    public static VertexSorter sortByNormalRelative(Vector3f normal) {
        return new SortNormalRelative(normal);
    }

    private static class SortByDistance extends AbstractVertexSorter {
        private final Vector3f origin;

        private SortByDistance(Vector3f origin) {
            this.origin = origin;
        }

        @Override
        protected float getKey(Vector3f position) {
            float distance = Math.abs(position.x - this.origin.x);
            distance += Math.abs(position.y - this.origin.y);
            distance += Math.abs(position.z - this.origin.z);
            return distance;
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

    private static class SortNormalRelative extends AbstractVertexSorter {
        private final Vector3f normal;

        private SortNormalRelative(Vector3f normal) {
            this.normal = normal;
        }

        @Override
        protected float getKey(Vector3f position) {
            return this.normal.dot(position);
        }
    }

    private static abstract class AbstractVertexSorter implements VertexSorter {
        @Override
        public final int[] sort(Vector3f[] positions) {
            return this.mergeSort(positions);
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
