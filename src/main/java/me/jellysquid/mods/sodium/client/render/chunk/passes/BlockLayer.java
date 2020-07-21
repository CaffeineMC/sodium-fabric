package me.jellysquid.mods.sodium.client.render.chunk.passes;

import it.unimi.dsi.fastutil.objects.Reference2IntArrayMap;
import net.minecraft.client.render.RenderLayer;

// TODO: Move away from using an enum, make this extensible
public enum BlockLayer {
    SOLID_MIPPED(true),
    SOLID(false),
    TRANSLUCENT_MIPPED(true);

    public static final BlockLayer[] VALUES = BlockLayer.values();
    public static final int COUNT = VALUES.length;

    private final boolean mipped;

    BlockLayer(boolean mipped) {
        this.mipped = mipped;
    }

    public boolean isMipped() {
        return this.mipped;
    }

    public int getExpectedSize() {
        return 2097152; // TODO: tweak this
    }

    private static final Reference2IntArrayMap<RenderLayer> layerMappings;

    static {
        layerMappings = new Reference2IntArrayMap<>();
        layerMappings.defaultReturnValue(-1);

        layerMappings.put(RenderLayer.getSolid(), BlockLayer.SOLID_MIPPED.ordinal());
        layerMappings.put(RenderLayer.getCutoutMipped(), BlockLayer.SOLID_MIPPED.ordinal());
        layerMappings.put(RenderLayer.getCutout(), BlockLayer.SOLID.ordinal());
        layerMappings.put(RenderLayer.getTripwire(), BlockLayer.SOLID.ordinal());
        layerMappings.put(RenderLayer.getTranslucent(), BlockLayer.TRANSLUCENT_MIPPED.ordinal());
    }

    public static int fromRenderLayer(RenderLayer layer) {
        int pass = layerMappings.getInt(layer);

        if (pass < 0) {
            throw new NullPointerException("No render pass exists for layer: " + layer);
        }

        return pass;
    }
}
