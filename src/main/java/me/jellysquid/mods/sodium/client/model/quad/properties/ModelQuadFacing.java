package me.jellysquid.mods.sodium.client.model.quad.properties;

import net.minecraft.util.math.Direction;

public enum ModelQuadFacing {
    UP,
    DOWN,
    EAST,
    WEST,
    SOUTH,
    NORTH,
    UNASSIGNED;

    public static final ModelQuadFacing[] VALUES = ModelQuadFacing.values();
    public static final ModelQuadFacing[] DIRECTIONS = new ModelQuadFacing[] { UP, DOWN, EAST, WEST, SOUTH, NORTH };

    public static final int COUNT = VALUES.length;

    public static ModelQuadFacing fromDirection(Direction dir) {
        return switch (dir) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }
}
