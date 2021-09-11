package me.jellysquid.mods.sodium.model.quad.properties;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ModelQuadFacingBits {
    public static final int UP_BITS = bitfield(ModelQuadFacing.UP);
    public static final int DOWN_BITS = bitfield(ModelQuadFacing.DOWN);
    public static final int EAST_BITS = bitfield(ModelQuadFacing.EAST);
    public static final int WEST_BITS = bitfield(ModelQuadFacing.WEST);
    public static final int SOUTH_BITS = bitfield(ModelQuadFacing.SOUTH);
    public static final int NORTH_BITS = bitfield(ModelQuadFacing.NORTH);
    public static final int UNASSIGNED_BITS = bitfield(ModelQuadFacing.UNASSIGNED);
    public static final int ALL_BITS;

    static {
        var all = 0;

        for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
            all |= bitfield(facing);
        }

        ALL_BITS = all;
    }

    public static int bitfield(ModelQuadFacing facing) {
        return bitfield(facing.ordinal());
    }

    public static int bitfield(int face) {
        return 1 << face;
    }
}
