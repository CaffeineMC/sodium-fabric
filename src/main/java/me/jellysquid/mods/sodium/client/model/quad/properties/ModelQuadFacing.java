package me.jellysquid.mods.sodium.client.model.quad.properties;

import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.Validate;

public class ModelQuadFacing {
    public static final int POS_X = 0;
    public static final int POS_Y = 1;
    public static final int POS_Z = 2;

    public static final int NEG_X = 3;
    public static final int NEG_Y = 4;
    public static final int NEG_Z = 5;

    public static final int UNASSIGNED = 6;

    public static final int[] VALUES = new int[] { POS_X, POS_Y, POS_Z, NEG_X, NEG_Y, NEG_Z, UNASSIGNED };
    public static final int COUNT = 7;

    public static int fromDirection(Direction dir) {
        return switch (dir) {
            case DOWN -> NEG_Y;
            case UP -> POS_Y;
            case NORTH -> NEG_Z;
            case SOUTH -> POS_Z;
            case WEST -> NEG_X;
            case EAST -> POS_X;
        };
    }

    public static int getOpposite(int face) {
        return switch (face) {
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
