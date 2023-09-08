package me.jellysquid.mods.sodium.client.model.quad.properties;

import net.minecraft.util.math.Direction;

public enum ModelQuadFacing {
    POS_X,
    POS_Y,
    POS_Z,
    NEG_X,
    NEG_Y,
    NEG_Z,
    UNASSIGNED;

    public static final ModelQuadFacing[] VALUES = ModelQuadFacing.values();

    public static final int COUNT = VALUES.length;
    public static final int DIRECTIONS = VALUES.length - 1;

    public static final int NONE = 0;
    public static final int ALL = (1 << COUNT) - 1;

    public static ModelQuadFacing fromDirection(Direction dir) {
        return switch (dir) {
            case DOWN   -> NEG_Y;
            case UP     -> POS_Y;
            case NORTH  -> NEG_Z;
            case SOUTH  -> POS_Z;
            case WEST   -> NEG_X;
            case EAST   -> POS_X;
        };
    }

    public ModelQuadFacing getOpposite() {
        return switch (this) {
            case POS_Y -> NEG_Y;
            case NEG_Y -> POS_Y;
            case POS_X -> NEG_X;
            case NEG_X -> POS_X;
            case POS_Z -> NEG_Z;
            case NEG_Z -> POS_Z;
            default -> UNASSIGNED;
        };
    }

    public int getSign() {
        return switch (this) {
            case POS_Y, POS_X, POS_Z -> 1;
            case NEG_Y, NEG_X, NEG_Z -> -1;
            default -> 0;
        };
    }

    public Direction toDirection() {
        return switch (this) {
            case POS_Y -> Direction.UP;
            case NEG_Y -> Direction.DOWN;
            case POS_X -> Direction.EAST;
            case NEG_X -> Direction.WEST;
            case POS_Z -> Direction.SOUTH;
            case NEG_Z -> Direction.NORTH;
            default -> Direction.UP;
        };
    }
}
