package net.caffeinemc.mods.sodium.client.render.chunk.occlusion;

import net.minecraft.core.Direction;

public class GraphDirection {
    public static final int DOWN    = 0;
    public static final int UP      = 1;
    public static final int NORTH   = 2;
    public static final int SOUTH   = 3;
    public static final int WEST    = 4;
    public static final int EAST    = 5;


    public static final int COUNT   = 6;

    private static final Direction[] ENUMS;
    private static final int[] OPPOSITE;
    private static final int[] X, Y, Z;

    static {
        OPPOSITE = new int[COUNT];
        OPPOSITE[DOWN] = UP;
        OPPOSITE[UP] = DOWN;
        OPPOSITE[NORTH] = SOUTH;
        OPPOSITE[SOUTH] = NORTH;
        OPPOSITE[WEST] = EAST;
        OPPOSITE[EAST] = WEST;

        X = new int[COUNT];
        X[WEST] = -1;
        X[EAST] = 1;

        Y = new int[COUNT];
        Y[DOWN] = -1;
        Y[UP] = 1;

        Z = new int[COUNT];
        Z[NORTH] = -1;
        Z[SOUTH] = 1;

        ENUMS = new Direction[COUNT];
        ENUMS[DOWN] = Direction.DOWN;
        ENUMS[UP] = Direction.UP;
        ENUMS[NORTH] = Direction.NORTH;
        ENUMS[SOUTH] = Direction.SOUTH;
        ENUMS[WEST] = Direction.WEST;
        ENUMS[EAST] = Direction.EAST;
    }

    public static int opposite(int direction) {
        return OPPOSITE[direction];
    }

    public static int x(int direction) {
        return X[direction];
    }

    public static int y(int direction) {
        return Y[direction];
    }

    public static int z(int direction) {
        return Z[direction];
    }

    public static Direction toEnum(int direction) {
        return ENUMS[direction];
    }
}