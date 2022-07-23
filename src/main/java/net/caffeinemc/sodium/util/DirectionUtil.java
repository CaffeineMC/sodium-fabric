package net.caffeinemc.sodium.util;

import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.Validate;

/**
 * Contains a number of cached arrays to avoid allocations since calling Enum#values() requires the backing array to
 * be cloned every time.
 */
public class DirectionUtil {
    public static final Direction[] ALL_DIRECTIONS = Direction.values();
    public static final int COUNT = ALL_DIRECTIONS.length;

    // Provides the same order as enumerating Direction and checking the axis of each value
    public static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };

    public static final int DOWN = 0;
    public static final int UP = 1;
    public static final int NORTH = 2;
    public static final int SOUTH = 3;
    public static final int WEST = 4;
    public static final int EAST = 5;

    public static final int X_PLUS = EAST;
    public static final int X_MIN = WEST;
    public static final int Y_PLUS = UP;
    public static final int Y_MIN = DOWN;
    public static final int Z_PLUS = SOUTH;
    public static final int Z_MIN = NORTH;

    static {
        Validate.isTrue(DOWN == Direction.DOWN.getId());
        Validate.isTrue(UP == Direction.UP.getId());
        Validate.isTrue(NORTH == Direction.NORTH.getId());
        Validate.isTrue(SOUTH == Direction.SOUTH.getId());
        Validate.isTrue(WEST == Direction.WEST.getId());
        Validate.isTrue(EAST == Direction.EAST.getId());

        Validate.isTrue(Direction.EAST.getOffsetX() == 1);
        Validate.isTrue(Direction.WEST.getOffsetX() == -1);
        Validate.isTrue(Direction.UP.getOffsetY() == 1);
        Validate.isTrue(Direction.DOWN.getOffsetY() == -1);
        Validate.isTrue(Direction.SOUTH.getOffsetZ() == 1);
        Validate.isTrue(Direction.NORTH.getOffsetZ() == -1);
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
