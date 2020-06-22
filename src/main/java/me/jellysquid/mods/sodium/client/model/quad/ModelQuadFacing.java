package me.jellysquid.mods.sodium.client.model.quad;

import net.minecraft.util.math.Direction;

public enum ModelQuadFacing {
    UP,
    DOWN,
    NORTH,
    SOUTH,
    EAST,
    WEST,
    NONE;

    public static final ModelQuadFacing[] VALUES = ModelQuadFacing.values();
    public static final int COUNT = VALUES.length;
    public static final int[] BITS = new int[COUNT];

    static {
        for (int i = 0; i < BITS.length; i++) {
            BITS[i] = 1 << i;
        }
    }

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
                return NONE;
        }
    }
}
