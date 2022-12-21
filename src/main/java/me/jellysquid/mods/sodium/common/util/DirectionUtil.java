package me.jellysquid.mods.sodium.common.util;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

/**
 * Contains a number of cached arrays to avoid allocations since calling Enum#values() requires the backing array to
 * be cloned every time.
 */
public class DirectionUtil {

    public static final int DOWN = Direction.DOWN.ordinal();
    public static final int UP = Direction.UP.ordinal();
    public static final int NORTH = Direction.NORTH.ordinal();
    public static final int SOUTH = Direction.SOUTH.ordinal();
    public static final int WEST = Direction.WEST.ordinal();
    public static final int EAST = Direction.EAST.ordinal();

    public static final int COUNT = 6;

    public static final Direction[] ENUMS;
    public static final Direction.Axis[] AXIS;
    public static final Vec3i[] VECTOR;
    public static final int[] OPPOSITE;

    static {
        ENUMS = new Direction[COUNT];
        AXIS = new Direction.Axis[COUNT];
        OPPOSITE = new int[COUNT];
        VECTOR = new Vec3i[COUNT];

        for (int id = 0; id < COUNT; id++) {
            var dir = Direction.byId(id);
            ENUMS[id] = dir;
            AXIS[id] = dir.getAxis();
            VECTOR[id] = dir.getVector();
            OPPOSITE[id] = dir.getOpposite()
                    .getId();
        }
    }

    // Provides the same order as enumerating Direction and checking the axis of each value
    public static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };

    public static Direction getEnum(int dir) {
        return DirectionUtil.ENUMS[dir];
    }

    public static Vec3i getOffset(int dir) {
        return DirectionUtil.VECTOR[dir];
    }

    public static int getOffsetX(int dir) {
        return switch (dir) {
            case 4 -> -1;
            case 5 -> 1;
            default -> 0;
        };
    }

    public static int getOffsetY(int dir) {
        return switch (dir) {
            case 0 -> -1;
            case 1 -> 1;
            default -> 0;
        };
    }

    public static int getOffsetZ(int dir) {
        return switch (dir) {
            case 2 -> -1;
            case 3 -> 1;
            default -> 0;
        };
    }

    public static int getOpposite(int dir) {
        return switch (dir) {
            case 0 -> 1;
            case 1 -> 0;
            case 2 -> 3;
            case 3 -> 2;
            case 4 -> 5;
            case 5 -> 4;
            default -> dir;
        };
    }

    public static Direction.Axis getAxis(int dir) {
        return DirectionUtil.AXIS[dir];
    }
}
