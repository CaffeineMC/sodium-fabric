package me.jellysquid.mods.sodium.interop.vanilla.mixin;

import net.minecraft.client.model.geom.ModelPart;

public interface ModelCubeAccessor {
    ModelPart.Polygon[] getQuads();
}
