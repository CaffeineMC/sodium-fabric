package me.jellysquid.mods.sodium.client.model.quad.properties;

import net.minecraft.util.math.Direction;

public enum ModelQuadFacing {
    DOWN,
    UP,
    NORTH,
    SOUTH,
    WEST,
    EAST,
    UNASSIGNED;

    public static final ModelQuadFacing[] VALUES = ModelQuadFacing.values();
    public static final ModelQuadFacing[] DIRECTIONS = new ModelQuadFacing[] { DOWN, UP, NORTH, SOUTH, WEST, EAST };

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
