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
    public static final int COUNT = VALUES.length;

    public static ModelQuadFacing fromDirection(Direction dir) {
        switch (dir) {
            case DOWN:
                return DOWN;
            case UP:
                return UP;
            case NORTH:
                return NORTH;
            case SOUTH:
                return SOUTH;
            case WEST:
                return WEST;
            case EAST:
                return EAST;
            default:
                return UNASSIGNED;
        }
    }

    public ModelQuadFacing getOpposite() {
        switch (this) {
            case UP:
                return DOWN;
            case DOWN:
                return UP;
            case EAST:
                return WEST;
            case WEST:
                return EAST;
            case SOUTH:
                return NORTH;
            case NORTH:
                return SOUTH;
            default:
                return UNASSIGNED;
        }
    }
}
