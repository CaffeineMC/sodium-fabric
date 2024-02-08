package net.caffeinemc.mods.sodium.client.model.quad.properties;

import net.minecraft.core.Direction;

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
}
