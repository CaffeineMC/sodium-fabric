package me.jellysquid.mods.sodium.common.util;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.Validate;

/**
 * Contains a number of cached arrays to avoid allocations since calling Enum#values() requires the backing array to
 * be cloned every time.
 */
@SuppressWarnings("ConstantValue")
public class DirectionUtil {

    public static final int DOWN = 0;
    public static final int UP = 1;
    public static final int NORTH = 2;
    public static final int SOUTH = 3;
    public static final int WEST = 4;
    public static final int EAST = 5;

    static {
        Validate.isTrue(DOWN == Direction.DOWN.ordinal());
        Validate.isTrue(UP == Direction.UP.ordinal());
        Validate.isTrue(NORTH == Direction.NORTH.ordinal());
        Validate.isTrue(SOUTH == Direction.SOUTH.ordinal());
        Validate.isTrue(WEST == Direction.WEST.ordinal());
        Validate.isTrue(EAST == Direction.EAST.ordinal());

        for (Direction dir : Direction.values()) {
            if (dir.ordinal() != dir.getId()) {
                throw new IllegalStateException("The ordinal for direction %s does not equal id".formatted(dir.name()));
            }
        }
    }

    public static final int COUNT = 6;

    static {
        Validate.isTrue(COUNT == Direction.values().length);
    }

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

    public static Direction.Axis getAxis(int dir) {
        return DirectionUtil.AXIS[dir];
    }

    public static int getOffsetX(int dir) {
        return switch (dir) {
            case WEST -> -1;
            case EAST -> 1;
            default -> 0;
        };
    }

    public static int getOffsetY(int dir) {
        return switch (dir) {
            case DOWN -> -1;
            case UP -> 1;
            default -> 0;
        };
    }

    public static int getOffsetZ(int dir) {
        return switch (dir) {
            case NORTH -> -1;
            case SOUTH -> 1;
            default -> 0;
        };
    }

    public static int getOpposite(int dir) {
        return switch (dir) {
            case DOWN -> UP;
            case UP -> DOWN;
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case EAST -> WEST;
            default -> dir;
        };
    }
}
