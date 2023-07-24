package me.jellysquid.mods.sodium.client.render.chunk.graph;

public class GraphDirection {
    public static final int DOWN    = 0;
    public static final int UP      = 1;
    public static final int NORTH   = 2;
    public static final int SOUTH   = 3;
    public static final int WEST    = 4;
    public static final int EAST    = 5;


    public static final int COUNT   = 6;

    public static final int NONE    = 0b000000;
    public static final int ALL     = 0b111111;

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
}