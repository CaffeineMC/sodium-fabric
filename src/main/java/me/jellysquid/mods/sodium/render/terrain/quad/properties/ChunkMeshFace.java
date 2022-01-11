package me.jellysquid.mods.sodium.render.terrain.quad.properties;

import net.minecraft.util.math.Direction;

public enum ChunkMeshFace {
    UP,
    DOWN,
    EAST,
    WEST,
    SOUTH,
    NORTH,
    UNASSIGNED;

    public static final ChunkMeshFace[] VALUES = ChunkMeshFace.values();
    public static final ChunkMeshFace[] DIRECTIONS = new ChunkMeshFace[] { UP, DOWN, EAST, WEST, SOUTH, NORTH };

    public static final int COUNT = VALUES.length;

    public static ChunkMeshFace fromDirection(Direction dir) {
        return switch (dir) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    public ChunkMeshFace getOpposite() {
        return switch (this) {
            case UP -> DOWN;
            case DOWN -> UP;
            case EAST -> WEST;
            case WEST -> EAST;
            case SOUTH -> NORTH;
            case NORTH -> SOUTH;
            default -> UNASSIGNED;
        };
    }

    public static final int UP_BITS = 1 << UP.ordinal();
    public static final int DOWN_BITS = 1 << DOWN.ordinal();
    public static final int EAST_BITS = 1 << EAST.ordinal();
    public static final int WEST_BITS = 1 << WEST.ordinal();
    public static final int SOUTH_BITS = 1 << SOUTH.ordinal();
    public static final int NORTH_BITS = 1 << NORTH.ordinal();
    public static final int UNASSIGNED_BITS = 1 << UNASSIGNED.ordinal();
    public static final int ALL_BITS = UP_BITS | DOWN_BITS | EAST_BITS | WEST_BITS | SOUTH_BITS | NORTH_BITS | UNASSIGNED_BITS;
}
