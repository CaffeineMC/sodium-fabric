package me.jellysquid.mods.sodium.client.render.light.smooth;

import net.minecraft.util.math.Direction;

@SuppressWarnings("UnnecessaryLocalVariable")
enum AoNeighborInfo {
    DOWN(new Direction[] { Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH }, 0.5F) {
        @Override
        public void calculateCornerWeights(float x, float y, float z, float[] out) {
            final float u = z;
            final float v = 1.0f - x;

            out[0] = v * u;
            out[1] = v * (1.0f - u);
            out[2] = (1.0f - v) * (1.0f - u);
            out[3] = (1.0f - v) * u;
        }

        @Override
        public void mapCorners(int[] lm0, float[] ao0, int[] lm1, float[] ao1) {
            lm1[0] = lm0[0];
            lm1[1] = lm0[1];
            lm1[2] = lm0[2];
            lm1[3] = lm0[3];

            ao1[0] = ao0[0];
            ao1[1] = ao0[1];
            ao1[2] = ao0[2];
            ao1[3] = ao0[3];
        }

        @Override
        public float getDepth(float x, float y, float z) {
            return y;
        }
    },
    UP(new Direction[] { Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH }, 1.0F) {
        @Override
        public void calculateCornerWeights(float x, float y, float z, float[] out) {
            final float u = z;
            final float v = x;

            out[0] = v * u;
            out[1] = v * (1.0f - u);
            out[2] = (1.0f - v) * (1.0f - u);
            out[3] = (1.0f - v) * u;
        }

        @Override
        public void mapCorners(int[] lm0, float[] ao0, int[] lm1, float[] ao1) {
            lm1[2] = lm0[0];
            lm1[3] = lm0[1];
            lm1[0] = lm0[2];
            lm1[1] = lm0[3];

            ao1[2] = ao0[0];
            ao1[3] = ao0[1];
            ao1[0] = ao0[2];
            ao1[1] = ao0[3];
        }

        @Override
        public float getDepth(float x, float y, float z) {
            return 1.0f - y;
        }
    },
    NORTH(new Direction[] { Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST }, 0.8F) {
        @Override
        public void calculateCornerWeights(float x, float y, float z, float[] out) {
            final float u = 1.0f - x;
            final float v = y;

            out[0] = v * u;
            out[1] = v * (1.0f - u);
            out[2] = (1.0f - v) * (1.0f - u);
            out[3] = (1.0f - v) * u;
        }

        @Override
        public void mapCorners(int[] lm0, float[] ao0, int[] lm1, float[] ao1) {
            lm1[3] = lm0[0];
            lm1[0] = lm0[1];
            lm1[1] = lm0[2];
            lm1[2] = lm0[3];

            ao1[3] = ao0[0];
            ao1[0] = ao0[1];
            ao1[1] = ao0[2];
            ao1[2] = ao0[3];
        }

        @Override
        public float getDepth(float x, float y, float z) {
            return z;
        }
    },
    SOUTH(new Direction[] { Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP }, 0.8F) {
        @Override
        public void calculateCornerWeights(float x, float y, float z, float[] out) {
            final float u = y;
            final float v = 1.0f - x;

            out[0] = u * v;
            out[1] = (1.0f - u) * v;
            out[2] = (1.0f - u) * (1.0f - v);
            out[3] = u * (1.0f - v);
        }

        @Override
        public void mapCorners(int[] lm0, float[] ao0, int[] lm1, float[] ao1) {
            lm1[0] = lm0[0];
            lm1[1] = lm0[1];
            lm1[2] = lm0[2];
            lm1[3] = lm0[3];

            ao1[0] = ao0[0];
            ao1[1] = ao0[1];
            ao1[2] = ao0[2];
            ao1[3] = ao0[3];
        }

        @Override
        public float getDepth(float x, float y, float z) {
            return 1.0f - z;
        }
    },
    WEST(new Direction[] { Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH }, 0.6F) {
        @Override
        public void calculateCornerWeights(float x, float y, float z, float[] out) {
            final float u = z;
            final float v = y;

            out[0] = v * u;
            out[1] = v * (1.0f - u);
            out[2] = (1.0f - v) * (1.0f - u);
            out[3] = (1.0f - v) * u;
        }

        @Override
        public void mapCorners(int[] lm0, float[] ao0, int[] lm1, float[] ao1) {
            lm1[3] = lm0[0];
            lm1[0] = lm0[1];
            lm1[1] = lm0[2];
            lm1[2] = lm0[3];

            ao1[3] = ao0[0];
            ao1[0] = ao0[1];
            ao1[1] = ao0[2];
            ao1[2] = ao0[3];
        }

        @Override
        public float getDepth(float x, float y, float z) {
            return x;
        }
    },
    EAST(new Direction[] { Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH }, 0.6F) {
        @Override
        public void calculateCornerWeights(float x, float y, float z, float[] out) {
            final float u = z;
            final float v = 1.0f - y;

            out[0] = v * u;
            out[1] = v * (1.0f - u);
            out[2] = (1.0f - v) * (1.0f - u);
            out[3] = (1.0f - v) * u;
        }

        @Override
        public void mapCorners(int[] lm0, float[] ao0, int[] lm1, float[] ao1) {
            lm1[1] = lm0[0];
            lm1[2] = lm0[1];
            lm1[3] = lm0[2];
            lm1[0] = lm0[3];

            ao1[1] = ao0[0];
            ao1[2] = ao0[1];
            ao1[3] = ao0[2];
            ao1[0] = ao0[3];
        }

        @Override
        public float getDepth(float x, float y, float z) {
            return 1.0f - x;
        }
    };

    public final Direction[] faces;

    public final float strength;

    AoNeighborInfo(Direction[] directions, float strength) {
        this.faces = directions;
        this.strength = strength;
    }

    public abstract void calculateCornerWeights(float x, float y, float z, float[] out);

    public abstract void mapCorners(int[] lm0, float[] ao0, int[] lm1, float[] ao1);

    public abstract float getDepth(float x, float y, float z);

    private static final AoNeighborInfo[] VALUES = AoNeighborInfo.values();

    public static AoNeighborInfo get(Direction direction) {
        return VALUES[direction.getId()];
    }
}
