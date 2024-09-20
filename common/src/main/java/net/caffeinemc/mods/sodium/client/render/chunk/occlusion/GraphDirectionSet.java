package net.caffeinemc.mods.sodium.client.render.chunk.occlusion;

public class GraphDirectionSet {
    public static final int NONE    = 0;
    public static final int ALL     = (1 << GraphDirection.COUNT) - 1;

    public static int of(int direction) {
        return 1 << direction;
    }

    public static boolean contains(int set, int direction) {
        return (set & GraphDirectionSet.of(direction)) != 0;
    }
}
