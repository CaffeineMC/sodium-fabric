package net.caffeinemc.sodium.util;

import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;

/**
 * Contains a number of cached arrays to avoid allocations since calling Enum#values() requires the backing array to
 * be cloned every time.
 */
public class DirectionUtil {
    public static final Direction[] ALL_DIRECTIONS = Direction.values();
    public static final int COUNT = ALL_DIRECTIONS.length;

    // Provides the same order as enumerating Direction and checking the axis of each value
    public static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };

    private static final int DOWN = 0;
    private static final int UP = 1;
    private static final int NORTH = 2;
    private static final int SOUTH = 3;
    private static final int WEST = 4;
    private static final int EAST = 5;

    static {
        Validate.isTrue(DOWN == Direction.DOWN.getId());
        Validate.isTrue(UP == Direction.UP.getId());
        Validate.isTrue(NORTH == Direction.NORTH.getId());
        Validate.isTrue(SOUTH == Direction.SOUTH.getId());
        Validate.isTrue(WEST == Direction.WEST.getId());
        Validate.isTrue(EAST == Direction.EAST.getId());
    }

    public static int getOppositeId(int dir) {
        return switch (dir) {
            case DOWN -> UP;
            case UP -> DOWN;
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case EAST -> WEST;
            default -> throw new IllegalStateException("Unexpected value: " + dir);
        };
    }
}
