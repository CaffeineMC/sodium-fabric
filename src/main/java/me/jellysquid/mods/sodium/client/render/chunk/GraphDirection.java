package me.jellysquid.mods.sodium.client.render.chunk;

public class GraphDirection {
    public static final int NONE = -1;

    public static final int DOWN = 0;
    public static final int UP = 1;
    public static final int NORTH = 2;
    public static final int SOUTH = 3;
    public static final int WEST = 4;
    public static final int EAST = 5;

    public static final int[] DIRECTIONS = { DOWN, UP, NORTH, SOUTH, WEST, EAST };
    public static final int COUNT = 6;

    public static int opposite(int dir) {
        return switch (dir) {
            case DOWN -> UP;
            case UP -> DOWN;
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case EAST -> WEST;
            default -> NONE;
        };
    }

    public static int x(int dir) {
        return switch (dir) {
            case WEST -> -1;
            case EAST -> 1;
            default -> 0;
        };
    }

    public static int y(int dir) {
        return switch (dir) {
            case DOWN -> -1;
            case UP -> 1;
            default -> 0;
        };
    }

    public static int z(int dir) {
        return switch (dir) {
            case NORTH -> -1;
            case SOUTH -> 1;
            default -> 0;
        };
    }
}
