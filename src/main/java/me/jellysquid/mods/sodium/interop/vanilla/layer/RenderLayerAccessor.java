package me.jellysquid.mods.sodium.interop.vanilla.layer;


public interface RenderLayerAccessor extends RenderPhaseAccessor {

    boolean getTranslucent(); // This doesn't actually mean translucent, yarn just doesn't have a good name for this.
}
