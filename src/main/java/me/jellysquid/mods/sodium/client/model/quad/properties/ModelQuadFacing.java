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

    public static final int BIT_UP = 1 << UP.ordinal();
    public static final int BIT_DOWN = 1 << DOWN.ordinal();
    public static final int BIT_EAST = 1 << EAST.ordinal();
    public static final int BIT_WEST = 1 << WEST.ordinal();
    public static final int BIT_SOUTH = 1 << SOUTH.ordinal();
    public static final int BIT_NORTH = 1 << NORTH.ordinal();
    public static final int BIT_UNASSIGNED = 1 << UNASSIGNED.ordinal();

    public static final int BIT_ALL = BIT_UP | BIT_DOWN | BIT_EAST | BIT_WEST | BIT_SOUTH | BIT_NORTH | BIT_UNASSIGNED;

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

    public ModelQuadFacing getOpposite() {
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
