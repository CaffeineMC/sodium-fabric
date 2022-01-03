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
}
