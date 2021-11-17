package me.jellysquid.mods.sodium.interop.vanilla.layer;

import net.minecraft.client.render.RenderLayer;

public interface MultiPhaseAccessor {
    RenderLayer.MultiPhaseParameters getPhases();
}
