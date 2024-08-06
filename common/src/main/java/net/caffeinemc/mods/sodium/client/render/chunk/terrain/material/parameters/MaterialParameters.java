package net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.parameters;

public class MaterialParameters {
    public static final int OFFSET_USE_MIP = 0;
    public static final int OFFSET_ALPHA_CUTOFF = 1;

    public static int pack(AlphaCutoffParameter alphaCutoff, boolean useMipmaps) {
        return (((useMipmaps ? 1 : 0) << OFFSET_USE_MIP) |
                ((alphaCutoff.ordinal()) << OFFSET_ALPHA_CUTOFF));
    }
}
