package me.jellysquid.mods.sodium.common.util;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

/**
 * Contains a number of cached arrays to avoid allocations since calling Enum#values() requires the backing array to
 * be cloned every time.
 */
public class DirectionUtil {
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

    public static int getOpposite(int dir) {
        return DirectionUtil.OPPOSITE[dir];
    }

    public static Direction.Axis getAxis(int dir) {
        return DirectionUtil.AXIS[dir];
    }
}
