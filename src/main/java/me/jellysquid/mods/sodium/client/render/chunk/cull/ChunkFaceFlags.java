package me.jellysquid.mods.sodium.client.render.chunk.cull;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;

public class ChunkFaceFlags {
    public static final int UP = of(ModelQuadFacing.UP);
    public static final int DOWN = of(ModelQuadFacing.DOWN);
    public static final int WEST = of(ModelQuadFacing.WEST);
    public static final int EAST = of(ModelQuadFacing.EAST);
    public static final int NORTH = of(ModelQuadFacing.NORTH);
    public static final int SOUTH = of(ModelQuadFacing.SOUTH);
    public static final int UNASSIGNED = of(ModelQuadFacing.UNASSIGNED);

    public static final int ALL = all();

    public static int of(ModelQuadFacing facing) {
        return 1 << facing.ordinal();
    }

    private static int all() {
        int v = 0;

        for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
            v |= of(facing);
        }

        return v;
    }
}
