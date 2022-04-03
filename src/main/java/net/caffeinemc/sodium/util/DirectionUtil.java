package net.caffeinemc.sodium.util;

import net.minecraft.util.math.Direction;

import java.util.Arrays;

/**
 * Contains a number of cached arrays to avoid allocations since calling Enum#values() requires the backing array to
 * be cloned every time.
 */
public class DirectionUtil {
    public static final Direction[] ALL_DIRECTIONS = Direction.values();

    public static final int[] ALL_DIRECTION_IDS = Arrays.stream(ALL_DIRECTIONS)
            .mapToInt(Enum::ordinal)
            .toArray();

    private static final Direction[] OPPOSITE_DIRECTIONS = Arrays.stream(ALL_DIRECTIONS)
            .map(Direction::getOpposite)
            .toArray(Direction[]::new);

    private static final int[] OPPOSITE_DIRECTION_IDS = Arrays.stream(OPPOSITE_DIRECTIONS)
            .mapToInt(Enum::ordinal)
            .toArray();

    // Provides the same order as enumerating Direction and checking the axis of each value
    public static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };

    // Direction#byId is slow in the absence of Lithium
    public static Direction getOpposite(Direction dir) {
        return OPPOSITE_DIRECTIONS[dir.ordinal()];
    }

    public static int getOppositeId(int dir) {
        return OPPOSITE_DIRECTION_IDS[dir];
    }
}
